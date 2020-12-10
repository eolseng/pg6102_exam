create table trips
(
    id          bigint generated by default as identity primary key,
    capacity    integer   not null check (capacity <= 2147483647 AND capacity >= 1),
    description varchar(255),
    end_time    timestamp not null,
    location    varchar(255),
    price       integer   not null check (price <= 2147483647 AND price >= 0),
    start_time  timestamp not null,
    title       varchar(255)
);