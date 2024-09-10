<h1 align="center">Find Me Bro</h1>

## Описание
__Find Me Bro__ - это поисковый движок. С его помощью вы найдёте нужную информацию на просторах интернета:)

## Как искать

### Dashboard
Вкладка открывается по умолчанию. На ней отображается общая статистика по всем сайтам, а также детальная статистика и статус по каждому из сайтов.
![Dashboard](https://github.com/user-attachments/assets/bb49650c-f58c-42e3-857b-978bc1deca48)

### Management 
Здесь находятся инструменты управления - запуск и остановка полной индексации (переиндексации), а также возможность добавить (обновить) отдельную страницу по ссылке.
![Management](https://github.com/user-attachments/assets/f4e63c22-32e4-4c75-8e98-fc1385d9b7e9)

### Search
На этой вкладке находится поле поиска, выпадающий список с выбором сайта для поиска, а при нажатии на кнопку «Найти» выводятся результаты.
![Search](https://github.com/user-attachments/assets/aa5b8f1c-e51e-49c0-9b0a-a1eb3e0c3c02)

## О проекте
Это Spring-приложение (JAR-файл, запускаемый на любом сервере или компьютере), работающее с локально установленной базой данных MySQL, имеющее простой веб-интерфейс и API через который им можно управлять и получать результаты поисковой выдачи по запросу.

### Для запуска проекта
* Скачайте проект себе на компьютер
* Установите базу данных MySQL(если её еще нет), а также создайте новую базу данных
  * Замените логин и пароль в файле конфигурации `src/resources/application.yml`:
  ```yaml
  spring:
    datasource:
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://localhost:3306/search_engine # база данных
      username: root # имя пользователя
      password: Kimk7FjT # пароль пользователя
  ```
* Загрузите библиотеки морфологии, ссылка [тут](https://drive.google.com/file/d/1WCOVc2hR6zIOUbebCm7_3SphfnFurm2_/view).
  Так как в этом проекте вручную добавлены зависимости, необходимо указать в файле подключения зависимостей, путь до загруженных файлов библиотек морфологии `searchengine-master/pom.xml`:
  ```xml
  <dependency>
  <groupId>org.apache.lucene.morphology</groupId>
  <artifactId>morph</artifactId>
  <version>1.5</version>
  <scope>system</scope>
  <systemPath>C:/Users/zoyag/.m2/repository/lucene/morphology/morph/1.5/morph-1.5.jar</systemPath> # путь до файла библиотеки
  </dependency>
  ```
* Можете запустить проект

