package com.dbsyncer.cli.command;

import com.dbsyncer.metadata.entity.DatabaseType;
import com.dbsyncer.metadata.entity.MigrationTask;
import com.dbsyncer.metadata.repository.MigrationTaskRepository;
import com.dbsyncer.transformations.types.ColumnSpec;
import com.dbsyncer.transformations.types.TypeMapper;
import com.dbsyncer.transformations.types.TypeMappingRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Command(
        name = "transform",
        description = "Data transformation utilities (type mapping, DDL preview)",
        mixinStandardHelpOptions = true,
        subcommands = {
                TransformCommand.DdlCommand.class,
                TransformCommand.MapCommand.class,
                TransformCommand.SchemaFromJsonCommand.class,
                TransformCommand.RulesTemplateCommand.class
        }
)
public class TransformCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("Transform utilities. Use 'dbsyncer transform --help' for subcommands.");
    }

    @Component
    @Command(name = "ddl", description = "Generate PostgreSQL DDL from source column specs")
    public static class DdlCommand implements Runnable {

        @Option(names = {"--source-db"}, required = true, description = "Source DB dialect (mysql, oracle, postgresql)")
        private String sourceDb;

        @Option(names = {"--table"}, required = true, description = "Target table name")
        private String table;

        @Option(names = {"--schema"}, description = "Target schema name (default: public)", defaultValue = "public")
        private String schema;

        @Parameters(arity = "0..*", paramLabel = "COLUMN", description = "Column spec e.g. id:bigint unsigned, name:varchar(255), flag:tinyint(1)")
        private List<String> columns;

        @Option(names = {"--columns-file"}, description = "Path to file with one COLUMN spec per line")
        private String columnsFile;

        @Option(names = {"--existing-file"}, description = "Path to file listing existing column names (one per line) to avoid re-adding in alter mode")
        private String existingFile;

        @Option(names = {"--pk"}, split = ",", description = "Primary key columns, comma separated")
        private List<String> primaryKeys;

        @Option(names = {"--if-not-exists"}, description = "Include IF NOT EXISTS")
        private boolean ifNotExists;

        @Option(names = {"--rules"}, description = "Path to YAML/JSON overrides file")
        private String rulesPath;

        @Option(names = {"--mode"}, description = "DDL mode: create|alter (default: create)", defaultValue = "create")
        private String mode;

        @Option(names = {"--timestamp-tz"}, description = "Timestamp time zone: with|without (default: without)", defaultValue = "without")
        private String timestampTz;

        @Option(names = {"--schema-only"}, description = "Output only column definitions (no CREATE/ALTER wrapper)")
        private boolean schemaOnly;

        @Option(names = {"--format"}, description = "Output format: text|json (default: text)", defaultValue = "text")
        private String format;

        @Option(names = {"--binary-as"}, description = "Binary type mapping for preview: bytea|text (default: bytea)", defaultValue = "bytea")
        private String binaryAs;

        private final TypeMappingRegistry registry = new TypeMappingRegistry();
        @Autowired
        private MigrationTaskRepository taskRepository;

        @Override
        public void run() {
            try {
                resolveDefaultsFromTask();
                TypeMapper mapper = registry.get(sourceDb);
                if (rulesPath != null && !rulesPath.isBlank()) {
                    var loader = new com.dbsyncer.transformations.types.overrides.TypeMappingLoader();
                    var overrides = loader.load(java.nio.file.Path.of(rulesPath));
                    mapper = new com.dbsyncer.transformations.types.overrides.OverridableTypeMapper(mapper, overrides.getOverrides());
                }
                List<String> pgCols = new ArrayList<>();
                record ColumnDef(String name, String type, boolean notNull) {}
                List<String> allSpecs = new ArrayList<>();
                if (columns != null) allSpecs.addAll(columns);
                if (columnsFile != null && !columnsFile.isBlank()) {
                    allSpecs.addAll(readSpecsFromFile(columnsFile));
                }
                if (allSpecs.isEmpty()) {
                    throw new IllegalArgumentException("No columns provided. Use positional COLUMN args or --columns-file.");
                }
                java.util.List<ColumnDef> jsonCols = new ArrayList<>();

                for (String spec : allSpecs) {
                    ColumnSpec col = parse(spec);
                    String pgType = mapper.mapToPostgres(col);
                    pgType = applyTimestampTzPreference(pgType, timestampTz);
                    pgType = applyBinaryPreference(pgType, binaryAs);
                    String colLine = quote(col.getName()) + " " + pgType + (col.isNullable() ? "" : " NOT NULL");
                    pgCols.add(colLine);
                    jsonCols.add(new ColumnDef(col.getName(), pgType, !col.isNullable()));
                }

                // If existing-file provided, filter out already existing columns (for alter/schema-only use cases)
                java.util.Set<String> existing = readExistingColumns(existingFile);
                if (!existing.isEmpty()) {
                    java.util.List<String> filtered = new ArrayList<>();
                    java.util.List<ColumnDef> filteredJson = new ArrayList<>();
                    for (int i = 0; i < pgCols.size(); i++) {
                        String name = jsonCols.get(i).name();
                        if (!existing.contains(name.toLowerCase(java.util.Locale.ROOT))) {
                            filtered.add(pgCols.get(i));
                            filteredJson.add(jsonCols.get(i));
                        }
                    }
                    pgCols = filtered;
                    jsonCols = filteredJson;
                }
                if ("json".equalsIgnoreCase(format)) {
                    java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
                    out.put("schema", schema);
                    out.put("table", table);
                    out.put("mode", mode);
                    out.put("columns", jsonCols);
                    if (primaryKeys != null && !primaryKeys.isEmpty()) out.put("primaryKey", primaryKeys);
                    if (rulesPath != null && !rulesPath.isBlank()) out.put("rules", rulesPath);
                    var om = new com.fasterxml.jackson.databind.ObjectMapper();
                    String json = om.writerWithDefaultPrettyPrinter().writeValueAsString(out);
                    if (outFile != null && !outFile.isBlank()) {
                        java.nio.file.Files.writeString(java.nio.file.Path.of(outFile), json);
                        System.out.println("Schema JSON written to: " + outFile);
                    } else {
                        System.out.println(json);
                    }
                    return;
                }

                String ddl;
                if (schemaOnly) {
                    ddl = String.join(System.lineSeparator(), pgCols);
                } else if ("alter".equalsIgnoreCase(mode)) {
                    List<String> alters = new ArrayList<>();
                    String prefix = "ALTER TABLE " + quote(schema) + "." + quote(table) + " ADD COLUMN "
                            + (ifNotExists ? "IF NOT EXISTS " : "");
                    for (String col : pgCols) {
                        alters.add(prefix + col + ";");
                    }
                    if (primaryKeys != null && !primaryKeys.isEmpty()) {
                        String pk = "ALTER TABLE " + quote(schema) + "." + quote(table) +
                                " ADD PRIMARY KEY (" + String.join(", ", primaryKeys.stream().map(TransformCommand.DdlCommand::quote).toList()) + ");";
                        alters.add(pk);
                    }
                    ddl = String.join(System.lineSeparator(), alters);
                } else {
                    ddl = "CREATE TABLE " + (ifNotExists ? "IF NOT EXISTS " : "") + quote(schema) + "." + quote(table) + " (\n    "
                            + String.join(",\n    ", pgCols)
                            + buildPkClause(primaryKeys)
                            + "\n);";
                }
                if (outFile != null && !outFile.isBlank()) {
                    java.nio.file.Files.writeString(java.nio.file.Path.of(outFile), ddl);
                    System.out.println("DDL written to: " + outFile);
                } else {
                    System.out.println(ddl);
                }
            } catch (Exception e) {
                System.err.println("Error generating DDL: " + e.getMessage());
                System.exit(1);
            }
        }

        private static String quote(String id) {
            return '"' + id.replace("\"", "\"\"") + '"';
        }

        private static final Pattern TYPE_WITH_PARAM = Pattern.compile("([a-zA-Z0-9_ ]+?)(\\(([^)]+)\\))?$");

        private static ColumnSpec parse(String spec) {
            String[] parts = spec.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid column spec: " + spec);
            }
            String name = parts[0].trim();
            String typeSpec = parts[1].trim();

            boolean notNull = false;
            if (typeSpec.toLowerCase(Locale.ROOT).contains(" not null")) {
                notNull = true;
                typeSpec = typeSpec.replaceAll("(?i)\\s+not\\s+null", "").trim();
            }

            boolean unsigned = typeSpec.toLowerCase(Locale.ROOT).contains(" unsigned");
            if (unsigned) {
                typeSpec = typeSpec.replaceAll("(?i)\\s+unsigned", "").trim();
            }

            Matcher m = TYPE_WITH_PARAM.matcher(typeSpec);
            if (!m.find()) {
                throw new IllegalArgumentException("Invalid type spec: " + typeSpec);
            }
            String base = m.group(1).trim();
            String params = m.group(3);
            Integer length = null, precision = null, scale = null;
            if (params != null) {
                String[] ps = params.split(",");
                if (ps.length == 1) {
                    length = tryParseInt(ps[0].trim());
                } else if (ps.length >= 2) {
                    precision = tryParseInt(ps[0].trim());
                    scale = tryParseInt(ps[1].trim());
                }
            }

            return ColumnSpec.builder()
                    .name(name)
                    .typeName(base)
                    .length(length)
                    .precision(precision)
                    .scale(scale)
                    .nullable(!notNull)
                    .unsigned(unsigned)
                    .build();
        }

        private static Integer tryParseInt(String v) {
            try { return Integer.valueOf(v); } catch (Exception e) { return null; }
        }

        private static List<String> readSpecsFromFile(String path) throws java.io.IOException {
            List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Path.of(path));
            List<String> specs = new ArrayList<>();
            for (String l : lines) {
                String s = l.trim();
                if (s.isEmpty() || s.startsWith("#")) continue;
                specs.add(s);
            }
            return specs;
        }

        private static java.util.Set<String> readExistingColumns(String path) throws java.io.IOException {
            java.util.Set<String> set = new java.util.HashSet<>();
            if (path == null || path.isBlank()) return set;
            for (String l : java.nio.file.Files.readAllLines(java.nio.file.Path.of(path))) {
                String s = l.trim();
                if (s.isEmpty() || s.startsWith("#")) continue;
                // Support either 'name' or 'name:...' format
                String name = s.split(":", 2)[0].trim();
                set.add(name.toLowerCase(java.util.Locale.ROOT));
            }
            return set;
        }

        private static String applyTimestampTzPreference(String pgType, String pref) {
            if (pgType == null) return null;
            String lt = pgType.toLowerCase(Locale.ROOT).trim();
            if (!lt.startsWith("timestamp")) return pgType;
            if ("with".equalsIgnoreCase(pref)) {
                return "timestamp with time zone";
            } else {
                return "timestamp without time zone";
            }
        }

        private static String applyBinaryPreference(String pgType, String pref) {
            if (pgType == null) return null;
            String lt = pgType.toLowerCase(Locale.ROOT).trim();
            if (!lt.equals("bytea")) return pgType;
            if ("text".equalsIgnoreCase(pref)) return "text";
            return pgType;
        }

        private static String buildPkClause(List<String> pks) {
            if (pks == null || pks.isEmpty()) return "";
            List<String> q = new ArrayList<>();
            for (String k : pks) {
                q.add(quote(k.trim()));
            }
            return ",\n    PRIMARY KEY (" + String.join(", ", q) + ")";
        }

        @Option(names = {"--out"}, description = "Write DDL to file path")
        private String outFile;

        @Option(names = {"--task"}, description = "Task ID or name to read defaults (source-db, rules)")
        private String taskIdentifier;

        private void resolveDefaultsFromTask() {
            if (taskIdentifier == null || taskIdentifier.isBlank()) return;
            Optional<MigrationTask> opt = findTask(taskIdentifier);
            if (opt.isEmpty()) return;
            MigrationTask task = opt.get();
            if (sourceDb == null || sourceDb.isBlank()) {
                DatabaseType dt = task.getSourceType();
                if (dt != null) sourceDb = dt.name().toLowerCase(Locale.ROOT);
            }
            if (rulesPath == null || rulesPath.isBlank()) {
                Map<String, Object> sp = task.getSourceProperties();
                if (sp != null && sp.get("typeMapping.rulesPath") != null) {
                    rulesPath = sp.get("typeMapping.rulesPath").toString();
                }
            }
        }

        private Optional<MigrationTask> findTask(String identifier) {
            try {
                UUID id = UUID.fromString(identifier);
                return taskRepository.findById(id);
            } catch (IllegalArgumentException e) {
                return taskRepository.findByTaskName(identifier);
            }
        }
    }

    @Component
    @Command(name = "map", description = "Map source column specs to PostgreSQL types (with optional rules)")
    public static class MapCommand implements Runnable {

        @Option(names = {"--source-db"}, required = true, description = "Source DB dialect (mysql, oracle, postgresql)")
        private String sourceDb;

        @Option(names = {"--rules"}, description = "Path to YAML/JSON overrides file")
        private String rulesPath;

        @Parameters(arity = "0..*", paramLabel = "COLUMN", description = "Column spec e.g. id:bigint unsigned, name:varchar(255)")
        private List<String> columns;

        @Option(names = {"--columns-file"}, description = "Path to file with one COLUMN spec per line")
        private String columnsFile;

        private final TypeMappingRegistry registry = new TypeMappingRegistry();
        @Autowired
        private MigrationTaskRepository taskRepository;

        @Option(names = {"--format"}, description = "Output format: text|json (default: text)", defaultValue = "text")
        private String format;

        @Override
        public void run() {
            try {
                resolveDefaultsFromTask();
                TypeMapper base = registry.get(sourceDb);
                TypeMapper mapper = base;
                if (rulesPath != null && !rulesPath.isBlank()) {
                    var loader = new com.dbsyncer.transformations.types.overrides.TypeMappingLoader();
                    var overrides = loader.load(java.nio.file.Path.of(rulesPath));
                    mapper = new com.dbsyncer.transformations.types.overrides.OverridableTypeMapper(base, overrides.getOverrides());
                }
                record MapEntry(String name, String sourceType, String postgresType, boolean nullable,
                                boolean unsigned, Integer length, Integer precision, Integer scale) {}
                java.util.List<MapEntry> entries = new java.util.ArrayList<>();
                List<String> allSpecs = new ArrayList<>();
                if (columns != null) allSpecs.addAll(columns);
                if (columnsFile != null && !columnsFile.isBlank()) {
                    allSpecs.addAll(DdlCommand.readSpecsFromFile(columnsFile));
                }
                if (allSpecs.isEmpty()) {
                    throw new IllegalArgumentException("No columns provided. Use positional COLUMN args or --columns-file.");
                }
                for (String spec : allSpecs) {
                    ColumnSpec col = DdlCommand.parse(spec);
                    String pgType = mapper.mapToPostgres(col);
                    if ("json".equalsIgnoreCase(format)) {
                        entries.add(new MapEntry(col.getName(), col.getTypeName(), pgType, col.isNullable(),
                                col.isUnsigned(), col.getLength(), col.getPrecision(), col.getScale()));
                    } else {
                        System.out.println(col.getName() + " -> " + pgType);
                    }
                }
                if ("json".equalsIgnoreCase(format)) {
                    var out = new java.util.LinkedHashMap<String, Object>();
                    out.put("sourceDb", sourceDb);
                    out.put("rules", (rulesPath != null && !rulesPath.isBlank()) ? rulesPath : null);
                    out.put("mappings", entries);
                    var om = new com.fasterxml.jackson.databind.ObjectMapper();
                    String json = om.writerWithDefaultPrettyPrinter().writeValueAsString(out);
                    System.out.println(json);
                }
            } catch (Exception e) {
                System.err.println("Error mapping types: " + e.getMessage());
                System.exit(1);
            }
        }

        @Option(names = {"--task"}, description = "Task ID or name to read defaults (source-db, rules)")
        private String taskIdentifier;

        private void resolveDefaultsFromTask() {
            if (taskIdentifier == null || taskIdentifier.isBlank()) return;
            Optional<MigrationTask> opt = findTask(taskIdentifier);
            if (opt.isEmpty()) return;
            MigrationTask task = opt.get();
            if (sourceDb == null || sourceDb.isBlank()) {
                DatabaseType dt = task.getSourceType();
                if (dt != null) sourceDb = dt.name().toLowerCase(Locale.ROOT);
            }
            if (rulesPath == null || rulesPath.isBlank()) {
                Map<String, Object> sp = task.getSourceProperties();
                if (sp != null && sp.get("typeMapping.rulesPath") != null) {
                    rulesPath = sp.get("typeMapping.rulesPath").toString();
                }
            }
        }

        private Optional<MigrationTask> findTask(String identifier) {
            try {
                UUID id = UUID.fromString(identifier);
                return taskRepository.findById(id);
            } catch (IllegalArgumentException e) {
                return taskRepository.findByTaskName(identifier);
            }
        }
    }

    @Component
    @Command(name = "schema", description = "Generate DDL from a Connect/Debezium JSON schema file")
    public static class SchemaFromJsonCommand implements Runnable {

        @Option(names = {"--file"}, required = true, description = "Path to JSON file containing Connect schema or Debezium envelope with schema (value)")
        private String file;

        @Option(names = {"--key-file"}, description = "Path to JSON file containing Connect key schema (for PK inference)")
        private String keyFile;

        @Option(names = {"--source-db"}, description = "Source DB dialect (mysql, oracle, postgresql)")
        private String sourceDb;

        @Option(names = {"--task"}, description = "Task ID or name to read defaults (source-db, rules)")
        private String taskIdentifier;

        @Option(names = {"--rules"}, description = "Path to YAML/JSON overrides file")
        private String rulesPath;

        @Option(names = {"--schema"}, description = "Target schema name (default: public)", defaultValue = "public")
        private String schemaName;

        @Option(names = {"--table"}, required = true, description = "Target table name")
        private String table;

        @Option(names = {"--mode"}, description = "DDL mode: create|alter (default: create)", defaultValue = "create")
        private String mode;

        @Option(names = {"--if-not-exists"}, description = "Include IF NOT EXISTS")
        private boolean ifNotExists;

        @Option(names = {"--timestamp-tz"}, description = "Timestamp time zone: with|without (default: without)", defaultValue = "without")
        private String timestampTz;

        @Option(names = {"--binary-as"}, description = "Binary type mapping for preview: bytea|text (default: bytea)", defaultValue = "bytea")
        private String binaryAs;

        @Option(names = {"--format"}, description = "Output format: text|json (default: text)", defaultValue = "text")
        private String format;

        @Option(names = {"--pk"}, split = ",", description = "Primary key columns, comma separated")
        private java.util.List<String> primaryKeys;

        @Autowired
        private com.dbsyncer.metadata.repository.MigrationTaskRepository taskRepository;

        @Override
        public void run() {
            try {
                resolveDefaultsFromTask();

                org.apache.kafka.connect.json.JsonConverter converter = new org.apache.kafka.connect.json.JsonConverter();
                java.util.Map<String, Object> cfg = new java.util.HashMap<>();
                cfg.put("schemas.enable", true);
                converter.configure(cfg, false);
                byte[] bytes = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(file));
                org.apache.kafka.connect.data.SchemaAndValue sav = converter.toConnectData("topic", bytes);
                org.apache.kafka.connect.data.Schema schema = extractRowSchema(sav.schema());

                if ((primaryKeys == null || primaryKeys.isEmpty()) && keyFile != null && !keyFile.isBlank()) {
                    byte[] keyBytes = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(keyFile));
                    org.apache.kafka.connect.data.SchemaAndValue keySav = converter.toConnectData("topic-key", keyBytes);
                    primaryKeys = inferPkFromKeySchema(keySav.schema());
                }

                // Build mapper with optional overrides
                com.dbsyncer.transformations.types.TypeMappingRegistry reg = new com.dbsyncer.transformations.types.TypeMappingRegistry();
                com.dbsyncer.transformations.types.TypeMapper base = reg.get(sourceDb);
                com.dbsyncer.transformations.types.TypeMapper mapper = base;
                if (rulesPath != null && !rulesPath.isBlank()) {
                    var loader = new com.dbsyncer.transformations.types.overrides.TypeMappingLoader();
                    var overrides = loader.load(java.nio.file.Path.of(rulesPath));
                    mapper = new com.dbsyncer.transformations.types.overrides.OverridableTypeMapper(base, overrides.getOverrides());
                }

                // Convert schema fields to Postgres column definitions
                com.dbsyncer.transformations.schema.SchemaConverter conv = new com.dbsyncer.transformations.schema.SchemaConverter();
                java.util.List<com.dbsyncer.transformations.schema.ColumnDefinition> cols = conv.toPostgresColumns(mapper, schema);

                // Apply preferences and format
                java.util.List<String> pgCols = new java.util.ArrayList<>();
                record ColumnDef(String name, String type, boolean notNull) {}
                java.util.List<ColumnDef> jsonCols = new java.util.ArrayList<>();
                for (var cd : cols) {
                    String t = applyTimestampTzPreference(cd.getPostgresType(), timestampTz);
                    t = applyBinaryPreference(t, binaryAs);
                    pgCols.add(quote(cd.getName()) + " " + t);
                    jsonCols.add(new ColumnDef(cd.getName(), t, false));
                }

                if ("json".equalsIgnoreCase(format)) {
                    var out = new java.util.LinkedHashMap<String, Object>();
                    out.put("schema", schemaName);
                    out.put("table", table);
                    out.put("mode", mode);
                    out.put("columns", jsonCols);
                    if (rulesPath != null && !rulesPath.isBlank()) out.put("rules", rulesPath);
                    if (primaryKeys != null && !primaryKeys.isEmpty()) out.put("primaryKey", primaryKeys);
                    var om = new com.fasterxml.jackson.databind.ObjectMapper();
                    String json = om.writerWithDefaultPrettyPrinter().writeValueAsString(out);
                    System.out.println(json);
                    return;
                }

                String ddl;
                if ("alter".equalsIgnoreCase(mode)) {
                    java.util.List<String> alters = new java.util.ArrayList<>();
                    String prefix = "ALTER TABLE " + quote(schemaName) + "." + quote(table) + " ADD COLUMN "
                            + (ifNotExists ? "IF NOT EXISTS " : "");
                    for (String col : pgCols) {
                        alters.add(prefix + col + ";");
                    }
                    if (primaryKeys != null && !primaryKeys.isEmpty()) {
                        String pk = "ALTER TABLE " + quote(schemaName) + "." + quote(table) +
                                " ADD PRIMARY KEY (" + String.join(", ", primaryKeys.stream().map(TransformCommand.DdlCommand::quote).toList()) + ");";
                        alters.add(pk);
                    }
                    ddl = String.join(System.lineSeparator(), alters);
                } else {
                    String pkClause = (primaryKeys != null && !primaryKeys.isEmpty())
                            ? ",\n    PRIMARY KEY (" + String.join(", ", primaryKeys.stream().map(TransformCommand.DdlCommand::quote).toList()) + ")"
                            : "";
                    ddl = "CREATE TABLE " + (ifNotExists ? "IF NOT EXISTS " : "") + quote(schemaName) + "." + quote(table) + " (\n    "
                            + String.join(",\n    ", pgCols)
                            + pkClause
                            + "\n);";
                }
                System.out.println(ddl);
            } catch (Exception e) {
                System.err.println("Error generating DDL from schema: " + e.getMessage());
                System.exit(1);
            }
        }

        private void resolveDefaultsFromTask() {
            if (taskIdentifier == null || taskIdentifier.isBlank()) return;
            java.util.Optional<com.dbsyncer.metadata.entity.MigrationTask> opt;
            try {
                java.util.UUID id = java.util.UUID.fromString(taskIdentifier);
                opt = taskRepository.findById(id);
            } catch (IllegalArgumentException e) {
                opt = taskRepository.findByTaskName(taskIdentifier);
            }
            if (opt.isEmpty()) return;
            var task = opt.get();
            if (sourceDb == null || sourceDb.isBlank()) {
                var dt = task.getSourceType();
                if (dt != null) sourceDb = dt.name().toLowerCase(java.util.Locale.ROOT);
            }
            if (rulesPath == null || rulesPath.isBlank()) {
                var sp = task.getSourceProperties();
                if (sp != null && sp.get("typeMapping.rulesPath") != null) {
                    rulesPath = sp.get("typeMapping.rulesPath").toString();
                }
            }
        }

        private static org.apache.kafka.connect.data.Schema extractRowSchema(org.apache.kafka.connect.data.Schema schema) {
            if (schema == null) throw new IllegalArgumentException("Schema is null in JSON");
            if (schema.type() == org.apache.kafka.connect.data.Schema.Type.STRUCT) {
                var after = schema.field("after");
                if (after != null && after.schema() != null && after.schema().type() == org.apache.kafka.connect.data.Schema.Type.STRUCT) {
                    return after.schema();
                }
                return schema;
            }
            throw new IllegalArgumentException("Unsupported schema type: " + schema.type());
        }

        private static java.util.List<String> inferPkFromKeySchema(org.apache.kafka.connect.data.Schema keySchema) {
            if (keySchema == null) return java.util.Collections.emptyList();
            if (keySchema.type() == org.apache.kafka.connect.data.Schema.Type.STRUCT) {
                java.util.List<String> names = new java.util.ArrayList<>();
                for (org.apache.kafka.connect.data.Field f : keySchema.fields()) {
                    names.add(f.name());
                }
                return names;
            }
            return java.util.Collections.emptyList();
        }

        private static String quote(String id) {
            return '"' + id.replace("\"", "\"\"") + '"';
        }

        private static String applyTimestampTzPreference(String pgType, String pref) {
            if (pgType == null) return null;
            String lt = pgType.toLowerCase(java.util.Locale.ROOT).trim();
            if (!lt.startsWith("timestamp")) return pgType;
            if ("with".equalsIgnoreCase(pref)) {
                return "timestamp with time zone";
            } else {
                return "timestamp without time zone";
            }
        }

        private static String applyBinaryPreference(String pgType, String pref) {
            if (pgType == null) return null;
            String lt = pgType.toLowerCase(java.util.Locale.ROOT).trim();
            if (!lt.equals("bytea")) return pgType;
            if ("text".equalsIgnoreCase(pref)) return "text";
            return pgType;
        }
    }

    @Component
    @Command(name = "rules", description = "Generate sample type-mapping rules template (YAML)")
    public static class RulesTemplateCommand implements Runnable {

        @Option(names = {"--source-db"}, required = true, description = "Source DB dialect (mysql, oracle, postgresql)")
        private String sourceDb;

        @Option(names = {"-o", "--out"}, description = "Write template to file path")
        private String outFile;

        @Override
        public void run() {
            try {
                String yaml = switch (sourceDb.toLowerCase(java.util.Locale.ROOT)) {
                    case "mysql" -> mysqlTemplate();
                    case "oracle" -> oracleTemplate();
                    case "postgresql", "postgres" -> postgresTemplate();
                    default -> throw new IllegalArgumentException("Unsupported source-db: " + sourceDb);
                };
                if (outFile != null && !outFile.isBlank()) {
                    java.nio.file.Files.writeString(java.nio.file.Path.of(outFile), yaml);
                    System.out.println("Rules template written to: " + outFile);
                } else {
                    System.out.println(yaml);
                }
            } catch (Exception e) {
                System.err.println("Error generating rules template: " + e.getMessage());
                System.exit(1);
            }
        }

        private static String mysqlTemplate() {
            return """
            sourceDb: mysql
            overrides:
              # Treat tinyint(1) as boolean
              - name: tinyint
                lengthEquals: 1
                target: boolean

              # Map unsigned bigint to numeric(20,0)
              - name: bigint
                unsigned: true
                target: numeric(20,0)

              # Ensure json maps to jsonb
              - name: json
                target: jsonb

              # Example: force all varbinary to text (preview-only)
              # - name: varbinary
              #   target: text

              # Example: normalize datetime to timestamp without tz
              # - name: datetime
              #   target: timestamp without time zone
            """.stripIndent();
        }

        private static String oracleTemplate() {
            return """
            sourceDb: oracle
            overrides:
              # Example: number(p,0) small precision to integer
              - name: number
                precisionEquals: 9
                scaleEquals: 0
                target: integer

              # Example: number(p,0) medium to bigint
              - name: number
                precisionEquals: 18
                scaleEquals: 0
                target: bigint

              # Example: raw to bytea (default behavior)
              - name: raw
                target: bytea
            """.stripIndent();
        }

        private static String postgresTemplate() {
            return """
            sourceDb: postgresql
            overrides:
              # Example: text to varchar(1024)
              # - name: text
              #   target: varchar(1024)
            """.stripIndent();
        }
    }
}
