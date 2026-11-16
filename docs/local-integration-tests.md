# Tests d'intégration Hanab Live local

Les tests d'intégration locaux utilisent le serveur Hanab Live déployé sur une URL configurable.
Les identifiants ne sont jamais écrits dans le code.

## Configuration

1. Copier `.env.example` vers `.env.local`.
2. Renseigner l'URL et les deux comptes de test dans `.env.local`.
3. Laisser `.env.local` non suivi par Git : il est ignoré par `.gitignore`.

Clés attendues :

| Clé | Usage |
| --- | --- |
| `HANAB_TEST_SERVER` | URL HTTP du serveur, par exemple `http://localhost:1212` |
| `HANAB_TEST_USER_A` | Nom du premier compte de test |
| `HANAB_TEST_PASSWORD_A` | Mot de passe du premier compte |
| `HANAB_TEST_USER_B` | Nom du second compte de test |
| `HANAB_TEST_PASSWORD_B` | Mot de passe du second compte |

Les comptes sont utilisés avec le protocole bot (`client=bot`) et sont créés automatiquement au
premier login par le test. Les noms d'utilisateur doivent respecter la limite de 15 caractères
du serveur. Les mots de passe peuvent donc être des valeurs locales dédiées au test.

## Exécution

Avec le serveur local démarré :

```powershell
$env:JAVA_HOME = 'C:\Users\maxim\.local\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd --batch-mode --no-transfer-progress "-Dhanab.integration.run=true" "-Dtest=LocalServerIntegrationTest" test
```

Le test couvre l'authentification des deux utilisateurs bot, l'ouverture du WebSocket bot, la
récupération de l'identité, le chargement du lobby, la création et la jointure d'une table, son
démarrage, le chargement automatique de l'état de partie après `tableStarted` et l'envoi d'une
action `play` suivie d'un `gameEvent`. Les tests d'intégration sont opt-in et ne sont donc pas exécutés par `verify` par défaut. Le serveur limite le coût des tentatives de mot de passe :
après plusieurs exécutions rapprochées, attendre la durée indiquée par le serveur avant de
relancer les tests. Si `.env.local` est absent, le test est automatiquement ignoré afin que la
suite unitaire reste exécutable sur une machine sans serveur.

## État du serveur local utilisé

Le serveur Docker a été reconstruit depuis la révision courante référencée par le projet.
Après redémarrage, il s'arrête actuellement avec `Client assets do not match the compiled
server revision`. La suite d'intégration est donc bien en place et opt-in, mais son passage
réel reste conditionné à la régénération des assets du dépôt serveur ; `verify` laisse ces
tests désactivés par défaut.
