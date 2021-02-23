create table if not exists orders (
    type varchar not null,
    quote_asset varchar(64) not null,
    base_asset varchar(64) not null,
    amount bigint not null,
    price bigint not null,
    fee_per_token bigint not null,
    box_id  varchar(64) primary key,
    box_value bigint not null,
    script varchar not null,
    pk varchar not null,
    ts bigint not null
);