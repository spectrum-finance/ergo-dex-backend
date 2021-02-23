create table if not exists markets (
    quote_asset varchar(64),
    base_asset  varchar(64),
    ticker      varchar not null,
    primary key (quote_asset, base_asset)
);

create table if not exists trades (
    tx_id       varchar(64) primary key,
    quote_asset varchar(64) not null,
    base_asset  varchar(64) not null,
    amount      bigint      not null,
    price       bigint      not null,
    fee         bigint      not null,
    ts          timestamp   not null
);