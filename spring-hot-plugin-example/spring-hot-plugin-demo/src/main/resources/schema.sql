-- 创建一张用户表，包含id，name，age,id是主键
CREATE TABLE IF NOT EXISTS usertb (
    id INTEGER AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    age INTEGER
);