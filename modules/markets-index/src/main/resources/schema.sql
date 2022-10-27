create domain public.hash32type as varchar(64);

create domain public.pubkey as varchar(66);

create domain public.address as varchar(64);

create domain public.ticker as varchar;

create table if not exists public.pools (
    pool_state_id public.hash32type primary key,
    pool_id public.hash32type not null,
    lp_id public.hash32type not null,
    lp_amount bigint not null,
    x_id public.hash32type not null,
    x_amount bigint not null,
    y_id public.hash32type not null,
    y_amount bigint not null,
    fee_num integer not null,
    gindex bigint not null,
    height bigint not null,
    protocol_version integer not null
);

alter table public.pools owner to ergo_admin;

create index pools__pool_id on public.pools using btree (pool_id);
create index pools__protocol_version on public.pools using btree (protocol_version);
create index pools__x_id on public.pools using btree (x_id);
create index pools__y_id on public.pools using btree (y_id);

create table if not exists public.swaps (
    order_id public.hash32type primary key,
    pool_id public.hash32type not null,
    pool_state_id public.hash32type,
    max_miner_fee bigint,
    timestamp bigint not null,
    input_id public.hash32type not null,
    input_value bigint not null,
    min_output_id public.hash32type not null,
    min_output_amount bigint not null,
    output_amount bigint,
    dex_fee_per_token_num bigint not null,
    dex_fee_per_token_denom bigint not null,
    redeemer public.pubkey not null,
    protocol_version integer not null,
    contract_version integer not null
);

alter table public.swaps owner to ergo_admin;

create index swaps__pool_id on public.swaps using btree (pool_id);
create index swaps__pool_state_id on public.swaps using btree (pool_state_id);
create index swaps__protocol_version on public.swaps using btree (protocol_version);
create index swaps__input_id on public.swaps using btree (input_id);
create index swaps__min_output_id on public.swaps using btree (min_output_id);

create table if not exists public.redeems (
    order_id public.hash32type primary key,
    pool_id public.hash32type not null,
    pool_state_id public.hash32type,
    max_miner_fee bigint,
    timestamp bigint not null,
    lp_id public.hash32type not null,
    lp_amount bigint not null,
    output_amount_x bigint,
    output_amount_y bigint,
    dex_fee bigint not null,
    redeemer public.pubkey not null,
    protocol_version integer not null,
    contract_version integer not null
);

alter table public.redeems owner to ergo_admin;

create index redeems__pool_id on public.redeems using btree (pool_id);
create index redeems__pool_state_id on public.redeems using btree (pool_state_id);
create index redeems__protocol_version on public.redeems using btree (protocol_version);
create index redeems__lp_id on public.redeems using btree (lp_id);

create table if not exists public.deposits (
    order_id public.hash32type primary key,
    pool_id public.hash32type not null,
    pool_state_id public.hash32type,
    max_miner_fee bigint,
    timestamp bigint not null,
    input_id_x public.hash32type not null,
    input_amount_x bigint not null,
    input_id_y public.hash32type not null,
    input_amount_y bigint not null,
    output_amount_lp bigint,
    dex_fee bigint not null,
    redeemer public.pubkey not null,
    protocol_version integer not null,
    contract_version integer not null
);

alter table public.deposits owner to ergo_admin;

create index deposits__pool_id on public.deposits using btree (pool_id);
create index deposits__pool_state_id on public.deposits using btree (pool_state_id);
create index deposits__protocol_version on public.deposits using btree (protocol_version);
create index deposits__input_id_x on public.deposits using btree (input_id_x);
create index deposits__input_id_y on public.deposits using btree (input_id_y);

create table if not exists public.assets (
    id public.hash32type primary key,
    ticker public.ticker,
    decimals integer
);

alter table public.assets owner to ergo_admin;

create index assets__ticker on public.assets using btree (ticker);

create table if not exists public.lq_locks (
    id public.hash32type primary key,
    deadline integer not null,
    token_id public.hash32type not null,
    amount bigint not null,
    redeemer public.address not null
);

alter table public.lq_locks owner to ergo_admin;

create index lq_locks__asset_id on public.lq_locks using btree (token_id);

create table if not exists public.blocks (
    id public.hash32type primary key,
    height integer not null,
    timestamp bigint not null
);

alter table public.blocks owner to ergo_admin;

create index blocks__height on public.blocks using btree (height);

create table if not exists public.order_executor_fee (
    pool_id public.hash32type not null,
    order_id public.hash32type not null,
    output_id public.hash32type primary key,
    address public.address not null,
    fee bigint not null,
    timestamp bigint not null
);

alter table public.order_executor_fee owner to ergo_admin;

create index order_executor__addr on public.order_executor_fee using btree (address);
create index order_executor__ts on public.order_executor_fee using btree (timestamp);

create sequence if not exists public.state_seq;

create table if not exists public.state (
    id Integer not null default nextval('state_seq'),
    address Text not null,
    pool_id Text not null,
    lp_id Text not null,
    box_id Text not null,
    tx_id Text not null,
    block_id Text not null,
    balance Text not null,
    timestamp BIGINT not null,
    weight Text not null,
    op Text not null,
    amount BIGINT not null,
    gap BIGINT not null,
    lpErg Text not null,
    txHeight BIGINT not null,
    poolStateId Text not null,
    PRIMARY KEY (address, box_id, op, pool_id)
);

GRANT USAGE, SELECT ON SEQUENCE state_seq TO ergo_admin;

alter table public.state owner to ergo_admin;

create table if not exists public.swaps_state (
    address Text not null,
    avg_time_use BIGINT not null,
    avg_erg_amount BIGINT not null,
    PRIMARY KEY (address)
);

alter table public.swaps_state owner to ergo_admin;