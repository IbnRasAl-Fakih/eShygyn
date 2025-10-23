create table users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    phone varchar(20),
    chat_id varchar(255)
);