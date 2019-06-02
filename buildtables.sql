create table Book (
	id SERIAL PRIMARY KEY,
	title varchar(100));
	
	
	
create table Author (
	id SERIAL PRIMARY key,
	auth_name varchar(200)
);


create table book_authorship (
	book_id bigint references book (id),
	author_id bigint references author (id)
);
create table Author_Award (
	id SERIAL PRIMARY key,
	author_id bigint REFERENCES author (id),
	award_desc varchar(300),
	monetary_reward bigint
);