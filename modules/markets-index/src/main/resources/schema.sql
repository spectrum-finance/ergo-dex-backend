create table if not exists swaps (
    pool_id varchar(64) not null,
    pool_state_id varchar(64) not null,
    max_miner_fee bigint not null,
    timestamp bigint not null,
    input_id varchar(64) not null,
    input_value bigint not null,
    input_ticker varchar(10),
    min_output_id varchar(64) not null,
    min_output_value bigint not null,
    min_output_ticker varchar(10),
    dex_fee_per_token_num bigint not null,
    dex_fee_per_token_denom bigint not null,
    p2pk varchar(64) not null,
);

create table if not exists redeems (
    pool_id varchar(64) not null,
    pool_state_id varchar(64) not null,
    max_miner_fee bigint not null,
    timestamp bigint not null,
    lp_id varchar(64) not null,
    lp_value bigint not null,
    lp_ticker varchar(10),
    dex_fee bigint not null,
    p2pk varchar(64) not null
);

create table if not exists deposits (
    pool_id varchar(64) not null,
    pool_state_id varchar(64) not null,
    max_miner_fee bigint not null,
    timestamp bigint not null,
    in_x_id varchar(64) not null,
    in_x_value bigint not null,
    in_x_ticker varchar(10),
    in_y_id varchar(64) not null,
    in_y_value bigint not null,
    in_y_ticker varchar(10),
    dex_fee bigint not null,
    p2pk varchar(64) not null
);
