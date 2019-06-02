
--We have some varied relationships here to help us
--investigate things thoroughly.
--a Book will have a many-to-many relationship with an Author
--and an Author can have a one-to-many relationship with an Author_Awards
create table Book (
	id bigint,
	title varchar(100));
	
create table book_authorship (
	book_id bigint,
	author_id bigint
);
	
create table Author (
	id bigint,
	auth_name varchar(200)
);

create table Author_Award (
	id bigint,
	author_id bigint,
	award_desc varchar(300),
	monetary_reward bigint
);
	