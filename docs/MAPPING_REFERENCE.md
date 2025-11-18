# 类型映射参考（MySQL/Oracle → PostgreSQL）

> 本表为默认策略摘要，具体行为可通过规则（overrides）与 CLI 预览偏好进行覆盖。

## MySQL → PostgreSQL

- 数值
  - `tinyint(1)` → `boolean`
  - `smallint` → `smallint`
  - `mediumint` → `integer`
  - `int`/`integer` → `integer`
  - `bigint` → `bigint`（unsigned → `numeric(20,0)`）
  - `decimal(p,s)`/`numeric(p,s)` → `numeric(p,s)`
  - `float` → `real`
  - `double`/`double precision` → `double precision`

- 字符/文本
  - `char(n)` → `char(n)`
  - `varchar(n)` → `varchar(n)`
  - `text`/`tinytext`/`mediumtext`/`longtext` → `text`

- 二进制
  - `binary`/`varbinary`/`blob`/`tinyblob`/`mediumblob`/`longblob` → `bytea`

- 时间
  - `date` → `date`
  - `datetime`/`timestamp` → `timestamp without time zone`（可在预览中设置 `--timestamp-tz`）
  - `time` → `time without time zone`

- 其他
  - `json` → `jsonb`
  - `enum`/`set` → `text`
  - `year` → `integer`

## Oracle → PostgreSQL

- 数值
  - `number(p,s)` → `numeric(p,s)`；当 `s=0` 且 `p≤9` → `integer`；`p≤18` → `bigint`；否则 `numeric(p,0)`

- 字符/文本
  - `varchar2(n)`/`nvarchar2(n)` → `varchar(n)`
  - `char(n)`/`nchar(n)` → `char(n)`
  - `clob`/`nclob` → `text`

- 二进制
  - `raw`/`blob` → `bytea`

- 时间与区间
  - `date` → `timestamp without time zone`
  - `timestamp with time zone` → `timestamp with time zone`
  - `timestamp` → `timestamp without time zone`
  - `interval year to month` → `interval year to month`
  - `interval day to second[(scale)]` → `interval day to second[(scale)]`

## 可配置与覆盖

- 规则（overrides）：见 `docs/rules/*.yaml` 或使用 `db-syncer transform rules` 生成模板
- 预览偏好：DDL 预览支持 `--timestamp-tz` 与 `--binary-as`
- 运行时 SMT：`ApplyTypeMapping` 提供时间/JSON/可变精度 decimal 的标准化

