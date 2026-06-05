---
name: sql-query-generator
description: SQL 查询生成器技能。当用户用自然语言描述数据查询需求时，自动生成对应的 SQL 语句。触发词：SQL、查询、数据库查询、生成SQL、写SQL、查询语句、SELECT、数据检索、表查询。
---

# SQL Query Generator

将用户的自然语言描述转换为准确的 SQL 查询语句。

## 核心工作流程

### 1. 理解用户需求

从用户描述中提取关键信息：
- **查询目标**：需要获取什么数据
- **涉及表**：可能涉及哪些表
- **筛选条件**：WHERE 子句的条件
- **排序要求**：ORDER BY 的字段和方向
- **分组聚合**：GROUP BY 和聚合函数
- **限制数量**：LIMIT 的值

### 2. 处理模糊需求

当用户描述不明确时，主动询问：
- 表名和字段名不明确时
- 筛选条件有歧义时
- 需要多表关联但关系不清晰时

### 3. 生成 SQL 语句

根据需求生成 SQL，遵循以下原则：
- 使用标准 SQL 语法（兼容主流数据库）
- 添加必要的注释说明
- 对复杂查询分步骤解释
- 提供优化建议（如有必要）

## SQL 生成模式

### 基础查询
```sql
-- 查询指定表的所有数据
SELECT * FROM table_name;

-- 查询指定字段
SELECT column1, column2 FROM table_name;

-- 带条件查询
SELECT * FROM table_name WHERE condition;
```

### 聚合查询
```sql
-- 统计数量
SELECT COUNT(*) FROM table_name WHERE condition;

-- 求和、平均值、最大、最小
SELECT SUM(column), AVG(column), MAX(column), MIN(column)
FROM table_name
GROUP BY group_column;
```

### 多表关联
```sql
-- 内连接
SELECT t1.col1, t2.col2
FROM table1 t1
INNER JOIN table2 t2 ON t1.id = t2.t1_id;

-- 左连接
SELECT t1.col1, t2.col2
FROM table1 t1
LEFT JOIN table2 t2 ON t1.id = t2.t1_id;
```

### 子查询
```sql
-- IN 子查询
SELECT * FROM table1
WHERE id IN (SELECT t1_id FROM table2 WHERE condition);

-- EXISTS 子查询
SELECT * FROM table1 t1
WHERE EXISTS (SELECT 1 FROM table2 t2 WHERE t2.t1_id = t1.id);
```

## 输出格式

生成 SQL 时，提供以下内容：

1. **SQL 语句**：格式化后的完整 SQL
2. **功能说明**：简要描述查询的功能
3. **注意事项**：可能的性能问题或优化建议

## 示例

**用户输入**：查询最近7天内下单金额超过1000元的用户列表，按消费总额降序排列

**生成结果**：
```sql
-- 查询最近7天消费超过1000元的用户，按消费总额降序
SELECT 
    u.user_id,
    u.username,
    SUM(o.amount) AS total_amount,
    COUNT(o.order_id) AS order_count
FROM users u
INNER JOIN orders o ON u.user_id = o.user_id
WHERE o.create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
GROUP BY u.user_id, u.username
HAVING SUM(o.amount) > 1000
ORDER BY total_amount DESC;
```

**说明**：
- 使用 INNER JOIN 连接用户表和订单表
- 使用 DATE_SUB 计算最近7天的日期范围
- 使用 HAVING 过滤聚合后的条件
- 按消费总额降序排列

## 高级功能

### 1. 性能优化建议
- 建议在 WHERE 条件字段上建立索引
- 避免使用 SELECT *，只查询需要的字段
- 大数据量时考虑分页查询

### 2. 多数据库适配
根据用户使用的数据库类型，调整语法：
- MySQL：使用 LIMIT 分页
- SQL Server：使用 TOP 或 OFFSET FETCH
- Oracle：使用 ROWNUM 或 FETCH FIRST
- PostgreSQL：使用 LIMIT OFFSET
