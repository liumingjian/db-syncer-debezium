package com.dbsyncer.transformations.types.overrides;

import com.dbsyncer.transformations.types.ColumnSpec;
import com.dbsyncer.transformations.types.TypeMapper;

import java.util.List;
import java.util.Locale;

public class OverridableTypeMapper implements TypeMapper {
    private final TypeMapper delegate;
    private final List<TypeMappingOverrides.Rule> rules;

    public OverridableTypeMapper(TypeMapper delegate, List<TypeMappingOverrides.Rule> rules) {
        this.delegate = delegate;
        this.rules = rules;
    }

    @Override
    public String mapToPostgres(ColumnSpec column) {
        if (rules != null) {
            for (TypeMappingOverrides.Rule r : rules) {
                if (matches(r, column)) {
                    return applyTemplate(r.getTarget(), column);
                }
            }
        }
        return delegate.mapToPostgres(column);
    }

    private static boolean matches(TypeMappingOverrides.Rule r, ColumnSpec c) {
        if (r.getName() != null) {
            String rn = r.getName().trim().toLowerCase(Locale.ROOT);
            String cn = c.getTypeName() != null ? c.getTypeName().trim().toLowerCase(Locale.ROOT) : "";
            if (!cn.startsWith(rn)) return false;
        }
        if (r.getUnsigned() != null && r.getUnsigned() != c.isUnsigned()) return false;
        if (r.getLengthEquals() != null) {
            if (c.getLength() == null || !r.getLengthEquals().equals(c.getLength())) return false;
        }
        if (r.getPrecisionEquals() != null) {
            if (c.getPrecision() == null || !r.getPrecisionEquals().equals(c.getPrecision())) return false;
        }
        if (r.getScaleEquals() != null) {
            if (c.getScale() == null || !r.getScaleEquals().equals(c.getScale())) return false;
        }
        return r.getTarget() != null && !r.getTarget().isBlank();
    }

    private static String applyTemplate(String template, ColumnSpec c) {
        String out = template;
        if (c.getLength() != null) out = out.replace("${length}", String.valueOf(c.getLength()));
        if (c.getPrecision() != null) out = out.replace("${precision}", String.valueOf(c.getPrecision()));
        if (c.getScale() != null) out = out.replace("${scale}", String.valueOf(c.getScale()));
        return out;
    }
}

