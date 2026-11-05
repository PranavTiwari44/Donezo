<div align="center">
  <h1>✅ Donezo</h1>
  <p><strong>A simple to-do app with a login, so everyone gets their own list.</strong></p>

  ![Java](https://img.shields.io/badge/Java-18-ED8B00?logo=openjdk&logoColor=white)
  ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3-6DB33F?logo=springboot&logoColor=white)
  ![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white)
  ![Bootstrap](https://img.shields.io/badge/Bootstrap-5-7952B3?logo=bootstrap&logoColor=white)
</div>

## About

A to-do web app I built with Spring Boot and JSP. Log in, add your tasks with a target date, and keep track of what's left.

## Features

- Log in and see only your own todos
- Add, edit and delete todos
- Pick a target date for each one
- Saved in a MySQL database

## Run it locally

You'll need Java 18 and Docker.

**1. Start MySQL**

```bash
docker run -d --name todos-mysql \
  -e MYSQL_ROOT_PASSWORD=dummypassword \
  -e MYSQL_USER=todos-user \
  -e MYSQL_PASSWORD=dummytodos \
  -e MYSQL_DATABASE=todos \
  -p 3306:3306 mysql:8
```

**2. Start the app**

```bash
git clone https://github.com/PranavTiwari44/ToDoWebApp.git
cd ToDoWebApp
./mvnw spring-boot:run
```

Then open [localhost:8080](http://localhost:8080) and log in with `Pranav` / `qwerty`.

## To do

- Add a sign-up page
- Add a mark as done button
