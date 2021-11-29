create table if not exists swaps (
    pool_id varchar(64) not null,
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

CREATE TABLE outputs
(
    box_id                  VARCHAR(64) NOT NULL,
    transaction_id          VARCHAR(64) NOT NULL,
    value                   BIGINT      NOT NULL,
    index                   INTEGER     NOT NULL,
    global_index            BIGINT      NOT NULL,
    creation_height         INTEGER     NOT NULL,
    settlement_height       INTEGER     NOT NULL,
    ergo_tree               VARCHAR     NOT NULL,
    address                 VARCHAR     NOT NULL,
    additional_registers    JSON        NOT NULL,
    PRIMARY KEY (box_id)
);

CREATE INDEX "outputs__box_id" ON outputs (box_id);
CREATE INDEX "outputs__transaction_id" ON outputs (transaction_id);
CREATE INDEX "outputs__address" ON outputs (address);
CREATE INDEX "outputs__ergo_tree" ON outputs (ergo_tree);

CREATE TABLE assets
(
    token_id  VARCHAR(64) NOT NULL,
    index     INTEGER     NOT NULL,
    amount    BIGINT      NOT NULL,
    name      VARCHAR,
    decimals  INTEGER,
    type      VARCHAR,
    PRIMARY KEY (token_id)
);

CREATE INDEX "assets__token_id" ON node_assets (token_id);


