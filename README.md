# Hibernate Research Project

This project is meant to improve my understanding of the differences between merge, persist, update, and save. I'd say it was pretty successful.
The meat of the thing is in the tests. I've made an effort to provide comments as to what the tests are doing and the peculiar behaviors they attempt to uncover.

To run this, run `docker-compose up` in the local-database folder - that will bring up the database (you must have
docker-compose installed.)
You need to connect to that database with the credentials `username = alex` and `password = alex` at `localhost:5432`.
Once you've connected, run the script in `buildtables.sql`.

At this point, you should be ready to read through the tests and run them, etc.

