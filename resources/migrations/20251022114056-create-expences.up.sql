create table expences (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid,
    category varchar(255),
    amount double precision,
    date timestamptz,
    foreign key (user_id) references users(id)
);