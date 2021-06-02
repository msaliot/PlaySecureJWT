# Playframework JWT Secure API

Please be kind as this project is just a test that I put at your disposal to help you.
It's not perfect, because of the time, but you can use pieces of code or improve it.

Project if you want to implement your own JWT security in Play framework with Cookie.

3 different points we can protect:
- Secure all api with HttpFilter on the path (User will need a token)
- Secure part of the api with HttpFilter depends on the user (here, the groups)
- Secure Action with different Rights (authAction with roles)

To login as a user and check current user
```bash
POST  /login        BODY{"login":"user1", "password": "AZERTY1"}
POST  /auth/me      -> return current user
```

As admin you can create new users (passwords are crypted with Bcrypt)
```bash
POST  /login        BODY {"login":"admin", "password": "123456"}
POST  /auth/create  BODY {"login": "mireille", "password": "mathieu", "groups": ["group1"], "roles": ["USER"]}
POST  /auth/logout  -> logout current user
```

You can get a post only if you are in the right group and you are a user.
It's to show how to have authorisation for users on different urls.
```bash
GET   /auth/group1/myposts/2
```

You can delete a post only if you are 'ADMIN'
```bash
DELETE /auth/group1/myposts/2
```
In UserController we create an **AuthAction** with 'ADMIN' authorisation.
**authAction** control the user "roles" to access

    def create(): Action[JsValue] = authAction.async("ADMIN", parse.json)
