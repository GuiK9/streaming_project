CREATE TABLE video (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    path_archive VARCHAR(500) NOT NULL
);

CREATE SEQUENCE video_file_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0;
