# SQL 语法参考

## 目录
1. [基础查询](#基础查询)
2. [条件过滤](#条件过滤)
3. [排序与分页](#排序与分页)
4. [聚合函数](#聚合函数)
5. [表连接](#表连接)
6. [子查询](#子查询)
7. [常用函数](#常用函数)

## 基础查询

### SELECT 语句
```sql
-- 查询所有字段
SELECT * FROM table_name;

-- 查询指定字段
SELECT col1, col2, col3 FROM table_name;

-- 使用别名
SELECT col1 AS alias1, col2 AS alias2 FROM table_name;

-- 去重
SELECT DISTINCT col1 FROM table_name;
```

## 条件过滤

### WHERE 子句
```sql
-- 等值查询
SELECT * FROM users WHERE status = 'active';

-- 范围查询
SELECT * FROM orders WHERE amount BETWEEN 100 AND 1000;

-- 列表查询
SELECT * FROM products WHERE category IN ('电子', '服装', '食品');

-- 模糊查询
SELECT * FROM users WHERE username LIKE '张%';

-- NULL 判断
SELECT * FROM users WHERE email IS NOT NULL;

-- 组合条件
SELECT * FROM orders 
WHERE status = 'completed' 
  AND amount > 100 
  AND create_time > '2024-01-01';
```

## 排序与分页

### ORDER BY
```sql
-- 单字段排序
SELECT * FROM products ORDER BY price DESC;

-- 多字段排序
SELECT * FROM orders 
ORDER BY create_time DESC, amount ASC;
```

### 分页
```sql
-- MySQL / PostgreSQL
SELECT * FROM products 
ORDER BY id 
LIMIT 10 OFFSET 20;

-- SQL Server
SELECT * FROM products 
ORDER BY id 
OFFSET 20 ROWS 
FETCH NEXT 10 ROWS ONLY;

-- Oracle
SELECT * FROM (
    SELECT t.*, ROWNUM rn FROM (
        SELECT * FROM products ORDER BY id
    ) t WHERE ROWNUM <= 30
) WHERE rn > 20;
```

## 聚合函数

### 常用聚合
```sql
-- 计数
SELECT COUNT(*) FROM users;
SELECT COUNT(DISTINCT category) FROM products;

-- 求和
SELECT SUM(amount) FROM orders WHERE status = 'completed';

-- 平均值
SELECT AVG(price) FROM products;

-- 最大/最小值
SELECT MAX(price), MIN(price) FROM products;
```

### GROUP BY 和 HAVING
```sql
-- 分组统计
SELECT category, COUNT(*) AS cnt, AVG(price) AS avg_price
FROM products
GROUP BY category;

-- 过滤分组结果
SELECT user_id, SUM(amount) AS total
FROM orders
GROUP BY user_id
HAVING SUM(amount) > 1000;
```

## 表连接

### INNER JOIN
```sql
SELECT u.username, o.order_id, o.amount
FROM users u
INNER JOIN orders o ON u.user_id = o.user_id;
```

### LEFT JOIN
```sql
SELECT u.username, COUNT(o.order_id) AS order_count
FROM users u
LEFT JOIN orders o ON u.user_id = o.user_id
GROUP BY u.username;
```

### 多表连接
```sql
SELECT u.username, o.order_id, p.product_name, oi.quantity
FROM users u
INNER JOIN orders o ON u.user_id = o.user_id
INNER JOIN order_items oi ON o.order_id = oi.order_id
INNER JOIN products p ON oi.product_id = p.product_id;
```

## 子查询

### 标量子查询
```sql
SELECT * FROM products 
WHERE price > (SELECT AVG(price) FROM products);
```

### IN 子查询
```sql
SELECT * FROM users 
WHERE user_id IN (
    SELECT DISTINCT user_id FROM orders WHERE amount > 500
);
```

### EXISTS 子查询
```sql
SELECT * FROM users u
WHERE EXISTS (
    SELECT 1 FROM orders o 
    WHERE o.user_id = u.user_id 
      AND o.create_time > '2024-01-01'
);
```

## 常用函数

### 字符串函数
```sql
SELECT CONCAT(first_name, ' ', last_name) AS full_name FROM users;
SELECT UPPER(email) FROM users;
SELECT LENGTH(username) FROM users;
SELECT SUBSTRING(phone, 1, 3) FROM users;
```

### 日期函数
```sql
-- MySQL
SELECT NOW();
SELECT DATE_FORMAT(create_time, '%Y-%m-%d') FROM orders;
SELECT DATE_SUB(CURDATE(), INTERVAL 7 DAY);
SELECT DATEDIFF(end_date, start_date) FROM projects;

-- PostgreSQL
SELECT CURRENT_DATE;
SELECT EXTRACT(YEAR FROM create_time) FROM orders;
```

### 条件函数
```sql
-- CASE WHEN
SELECT 
    username,
    CASE 
        WHEN level >= 10 THEN '高级'
        WHEN level >= 5 THEN '中级'
        ELSE '初级'
    END AS user_level
FROM users;

-- COALESCE (处理 NULL)
SELECT COALESCE(nickname, username) AS display_name FROM users;

-- IF (MySQL)
SELECT IF(amount > 100, '大额', '普通') AS order_type FROM orders;
```

## 常见查询模式

### 最新记录查询
```sql
-- 每个用户的最新订单
SELECT * FROM orders o1
WHERE create_time = (
    SELECT MAX(create_time) 
    FROM orders o2 
    WHERE o2.user_id = o1.user_id
);
```

### 排名查询
```sql
-- 销量排行
SELECT 
    product_name,
    sales_count,
    RANK() OVER (ORDER BY sales_count DESC) AS ranking
FROM products;
```

### 累计求和
```sql
SELECT 
    order_id,
    amount,
    SUM(amount) OVER (ORDER BY create_time) AS running_total
FROM orders;
