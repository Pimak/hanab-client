# État de couverture de la roadmap client

La roadmap est implémentée pour le périmètre retenu : un client Java 21 destiné
exclusivement aux utilisateurs bot et aligné sur la dernière révision du serveur
enregistrée dans `automation/server-revision.json`.

## Couverture livrée

- authentification bot, création de compte bot, identité, déconnexion et reconnexion ;
- HTTP public nécessaire aux bots, avec erreurs typées et configuration externe ;
- WebSocket structuré courant : requêtes `{id, request, data}`, réponses `{reply, data}`
  et événements `{event, data}` ;
- corrélation des réponses, délais d'attente, reconnexion avec backoff et heartbeat ;
- lobby, présence, tables, spectateurs, propriétaire, démarrage par identifiants bot,
  redémarrage, pause, votes et actions de partie ;
- état de partie, événements, notes, chat, relations, catalogue, tags et pagination ;
- replays JSON, navigation, replay partagé, suggestions et annotations ;
- tests unitaires, tests de fixtures et test d'intégration opt-in contre une URL configurée.

## Hors périmètre assumé

- aucun client humain, web ou `app` ;
- aucune compatibilité avec l'ancien protocole textuel ;
- division de table, création d'une partie depuis un curseur partiel et temps de revue,
  car la révision serveur courante ne publie pas ces opérations ;
- rendu UI, sons, préférences locales et gestion humaine des comptes.

## Vérification

Commande de vérification locale :

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

Le test d'intégration réel est volontairement opt-in :

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress "-Dhanab.integration.run=true" "-Dtest=LocalServerIntegrationTest" test
```

Les identifiants et l'URL sont lus depuis `.env.local`, jamais indexé par Git.
