# Testes locais

Anotações de ambiente para testar coisas que exigem **dois jogadores**, como as
ligações.

## Servidor de dev + dois clients na mesma máquina

1. `gradlew runServer` sobe um servidor de desenvolvimento em `run/`. Na primeira vez
   ele para pedindo para aceitar o EULA — abrir `run/eula.txt` e trocar `eula=false` por
   `eula=true`, rodar de novo.
2. Em `run/server.properties`, pôr `online-mode=false`. Sem isso o servidor tenta
   validar as contas pela Microsoft/Mojang, e não dá para logar duas contas premium ao
   mesmo tempo na mesma máquina. Em modo offline qualquer nome de usuário serve.
3. Abrir dois clients, em dois terminais, com nomes diferentes:
   ```
   gradlew runClient --args="--username Luiz"
   gradlew runClient --args="--username Ruthe"
   ```
4. Cada client conecta pelo multiplayer normal em `localhost:25565`.

**Para dar OP:** o terminal onde `runServer` está rodando é o console do servidor — digita
o comando ali e dá Enter, mesmo com o log rolando por cima:
```
op Jogador1
```
Ou, já dentro do jogo com um jogador operado, `/op Jogador2` pelo chat.

gradlew processResources