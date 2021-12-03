create table if not exists pools (
    pool_id varchar(64) not null,
    lp_id varchar(64) not null,
    lp_amount bigint not null,
    lp_ticker varchar(10),
    x_id varchar(64) not null,
    x_amount bigint not null,
    x_ticker varchar(10),
    y_id varchar(64) not null,
    y_amount bigint not null,
    y_ticker varchar(10),
    fee_num integer not null,
    box_id varchar(64) not null
);

create table if not exists swaps (
    order_id varchar not null,
    pool_id varchar(64) not null,
    pool_state_id varchar(64),
    max_miner_fee bigint not null,
    timestamp bigint not null,
    input_id varchar(64) not null,
    input_value bigint not null,
    input_ticker varchar(10),
    min_output_id varchar(64) not null,
    min_output_amount bigint not null,
    min_output_ticker varchar(10),
    output_amount bigint,
    dex_fee_per_token_num bigint not null,
    dex_fee_per_token_denom bigint not null,
    p2pk varchar(64) not null,
);

create table if not exists redeems (
    order_id varchar not null,
    pool_id varchar(64) not null,
    pool_state_id varchar(64),
    max_miner_fee bigint not null,
    timestamp bigint not null,
    lp_id varchar(64) not null,
    lp_amount bigint not null,
    lp_ticker varchar(10),
    output_amount_x bigint,
    output_amount_y bigint,
    dex_fee bigint not null,
    p2pk varchar(64) not null
);

create table if not exists deposits (
    order_id varchar not null,
    pool_id varchar(64) not null,
    pool_state_id varchar(64),
    max_miner_fee bigint not null,
    timestamp bigint not null,
    input_id_x varchar(64) not null,
    input_amount_x bigint not null,
    input_ticker_x varchar(10),
    input_id_y varchar(64) not null,
    input_amount_y bigint not null,
    input_ticker_y varchar(10),
    output_amount_lp bigint,
    dex_fee bigint not null,
    p2pk varchar(64) not null
);
