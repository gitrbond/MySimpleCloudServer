# Multiclient chat by Roman Bondar
Written on java with the help of sockets in IO style

Сервер поднимается на порте, указанном в файле server-properties.txt и ждет соединений по сокету. Запускается он из метода main() класса Server. Для хранения файлов у него заведена папка storage\

Клиент может подключиться к серверу (ip адрес и порт берутся из файла client-properties.txt), авторизоваться и посылать текстовые сообщения и файлы командами через терминал:
* connect <username> - авторизоваться на сервере. Без авторизации команды присылать нельзя
* send text <text> - отправить текстовое сообщение всем участникам чата
* send file <path_to_file> - отправить файл всем участникам. Он загрузится в хранилища файлов каждого пользователя
* quit - отключиться от сервера
  
Файлы клиента хранятся в папке username_files, где username - ник пользователя, под которым он зашел на сервер. Оттуда же они и загружаются. Запустить клиента можно через метод main() класса Client. При подключении в терминале отображается вся история сообщений, которая хранится на сервере, с отдельными сообщениями кто какой файл загрузил. Все файлы подгружаются в папку клиента.

# Пример работы
Пусть сервер уже запущен и история сообщений пуста. Подключается первый клиент, вот что он вводит у себя в терминале:
```
connect user1
send text Help, what happened to my computer?
send file comp.jpg
send text help
quit
```
Второй клиент подключается, видит историю сообщений, также к нему в соответствующую папку загружается файл comp.jpg. Теперь он вводит:
```
connect user2
send text hi
send text I think he has a disease and its terminal.
send text by the way check out my cat:
send file cat.jpg
quit
```
Снова подключается первый клиент, видит обновления и новую картинку кота у себя в папке.
```
connect user1
send text cool cat!
quit
```
  
При одновременном подключении клиентов поведение не меняется, но из Intellij Idea удобно только одного клиента запускать. Если есть способ легко запустить двух, скажите
