CREATE DATABASE SecuritySystem;
GO

USE SecuritySystem;
GO

CREATE TABLE dbo.SystemLogs (
    id_log INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    date_time DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    movement_level FLOAT NULL,
    image_path NVARCHAR(500) NULL
);
GO