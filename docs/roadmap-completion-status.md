# État de couverture de la roadmap client

Ce document complète `client-feature-inventory.md` avec l'état vérifié contre la
révision locale de `.hanabi-live` utilisée par les tests.

## Couverture livrée

Les parcours d'authentification bot/app, HTTP public, WebSocket, lobby, tables,
partie, notes, horloges, présence, chat, relations, catalogue, tags, replays JSON,
navigation de replay, hypothétiques, actions de replay partagé, suggestions,
reconnexion, heartbeat, métriques, validations locales et tests d'intégration sont
implémentés dans l'API Java et couverts par les tests unitaires ou
`LocalServerIntegrationTest`.

Le protocole bot reste explicite : `client=bot`, sans version web, avec le cycle
serveur `tableStarted → gameInit → gameState → gameLoaded`.

## Limites vérifiées du protocole courant

Les éléments suivants ne peuvent pas être ajoutés comme commandes autonomes sans
inventer un protocole qui n'existe pas dans la révision serveur locale :

- `TABLE-09` : aucune commande de division de table n'est publiée ;
- `REPLAY-09` : le serveur accepte une source sauvegardée/importée complète, mais
  pas la création d'une partie depuis un curseur partiel arbitraire ;
- `REPLAY-10` : aucun compteur de temps de revue n'est présent dans les messages
  ou snapshots courants ;
- la corrélation par identifiant de requête : le wire protocol n'envoie pas
  d'identifiant générique, donc `sendAndWait` sérialise les attentes et filtre les
  événements selon leur type/payload ;
- la négociation de version pour `app` et `bot` : le serveur réserve la validation
  de release au client `web` ; le client Java rejette volontairement une version
  web fournie au protocole bot.

## Vérification actuelle

La commande suivante est la porte de sortie locale :

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

Elle passe avec les tests unitaires, les tests de protocole, les tests HTTP et les
trois tests d'intégration contre `http://localhost:1212/` configurés par
`.env.local`.
