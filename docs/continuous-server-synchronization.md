# Synchronisation continue avec le serveur Hanab Live

## Objectif

Chaque version publiée de `hanab-client` doit correspondre à la version courante du serveur
Hanab Live au moment de sa publication. Les anciennes versions du serveur ne sont pas
maintenues et aucune matrice de compatibilité historique n'est nécessaire.

La mise à jour doit être automatique : une évolution du serveur déclenche une analyse, un travail
de synchronisation du client, des tests contre un serveur réellement lancé, puis une proposition
de changement accompagnée d'un changelog continu.

## Source de vérité et révision suivie

Le protocole du serveur reste la source de vérité. La révision de Hanab Live utilisée pour la
dernière synchronisation doit être enregistrée dans un fichier lisible par les outils, par exemple
`automation/server-revision.json` :

```json
{
  "repository": "https://github.com/Hanabi-Live/hanabi-live",
  "revision": "<server-commit>",
  "clientVersion": "<client-version>"
}
```

Le système compare cette révision avec la branche courante du serveur. S'il n'y a aucun changement
pertinent, le workflow s'arrête sans créer de modification. S'il existe un changement, il produit
un diff du protocole et lance une synchronisation.

## Exécution dans la CI

La surveillance est planifiée dans la CI, et non sur une machine personnelle. Le workflow doit
pouvoir fonctionner sans présence humaine et sans état local persistant :

```text
révision courante de hanab.live
              |
              v
détection du changement et diff du protocole
              |
              v
agent de synchronisation dans un worktree isolé
              |
              v
serveur Hanab Live jetable + tests Java
              |
              v
changelog + PR de synchronisation
              |
              v
validation CI puis publication
```

Le workflow planifié peut être quotidien ou plus fréquent. Il doit également pouvoir être lancé
manuellement pour diagnostiquer ou accélérer une synchronisation.

## Serveur de test éphémère

L'agent doit vérifier son travail contre un serveur Hanab Live réellement exécuté, et pas
uniquement contre des fixtures JSON.

Le runner CI doit donc :

1. récupérer la version courante du dépôt Hanab Live ;
2. démarrer un PostgreSQL jetable et isolé ;
3. construire les assets et le binaire Rust du serveur ;
4. lancer `hanabi-live` en HTTP local, sans TLS et sans données persistantes ;
5. attendre un endpoint de santé ou une condition de disponibilité ;
6. fournir l'URL locale aux tests Java, par exemple via `HANAB_TEST_SERVER` ;
7. arrêter le serveur et conserver ses logs en cas d'échec.

Le `compose.yml` du dépôt serveur fournit déjà PostgreSQL pour le développement. Le serveur
lui-même peut être lancé comme processus du runner, ce qui facilite la collecte des logs et le
diagnostic. Une image Docker complète pourra être ajoutée ultérieurement si le besoin de
reproductibilité le justifie.

Les scénarios d'intégration doivent couvrir au minimum l'authentification, le handshake
WebSocket, la création ou la jointure d'une table, une action de jeu et les événements associés.
Les tests locaux existants doivent être réutilisés afin d'éviter deux suites de comportement
différentes entre le développement et la CI.

## Agent indépendant du fournisseur

Le workflow ne doit pas dépendre de Codex, de Claude ou d'un autre fournisseur particulier. Il
doit appeler un adaptateur d'agent via une commande configurable, par exemple :

```text
HANAB_AGENT_PROVIDER=codex
HANAB_AGENT_COMMAND=...
```

ou :

```text
HANAB_AGENT_PROVIDER=custom
HANAB_AGENT_COMMAND=...
```

L'adaptateur reçoit un contexte structuré contenant :

- la révision serveur précédente et la révision cible ;
- le diff des sources de protocole ;
- le worktree de `hanab-client` ;
- les commandes de vérification obligatoires ;
- la consigne de ne modifier que le client, ses tests, ses fixtures et sa documentation.

Il retourne au minimum un statut, un résumé, la liste des zones modifiées et les résultats des
tests. L'agent peut itérer après un échec d'intégration, mais le nombre de tentatives et la durée
du job doivent être bornés.

L'agent travaille dans une branche ou un worktree temporaire. Il ne pousse jamais directement sur
`main` et ne reçoit aucun secret de production.

## Changelog continu et PR

Chaque synchronisation doit mettre à jour `docs/protocol-changelog.md`, avec une entrée décrivant :

- la révision serveur adoptée ;
- les messages, champs, commandes ou routes ajoutés, modifiés ou supprimés ;
- les adaptations effectuées dans le client ;
- les scénarios d'intégration exécutés ;
- les éventuelles limites ou décisions laissées à une intervention humaine.

Le workflow ouvre ou met à jour une PR contenant le diff du client, le changelog, les logs du
serveur en cas d'erreur et un rapport de compatibilité. La publication n'est autorisée qu'après
la réussite des tests unitaires, des tests de contrat et des tests d'intégration contre le serveur
courant.

Une défaillance de l'agent ne doit pas bloquer silencieusement la surveillance : elle doit créer ou
mettre à jour une issue avec la révision concernée, le diff, les logs et le dernier diagnostic
connu.

## Contraintes de sécurité et de fiabilité

- Utiliser exclusivement une base de données et des comptes de test jetables.
- Ne jamais connecter le workflow au serveur public ni à une base de production.
- Exécuter le code serveur et l'agent dans un runner isolé avec un timeout.
- Limiter les permissions du jeton CI au strict nécessaire pour créer une branche et une PR.
- Conserver les logs et les artefacts de diagnostic pour les synchronisations échouées.
- Rendre la synchronisation idempotente : relancer le workflow ne doit pas créer plusieurs PR
  concurrentes pour la même révision serveur.

## Découpage d'implémentation

1. Ajouter le fichier de révision serveur et les scripts de détection.
2. Rendre le démarrage du serveur local reproductible dans un script CI.
3. Paramétrer les tests Java d'intégration avec l'URL et les comptes de test du serveur jetable.
4. Définir le contrat d'entrée/sortie de l'adaptateur d'agent.
5. Ajouter le workflow planifié de synchronisation et de création de PR.
6. Générer le changelog continu et les rapports d'échec.
7. Ajouter la publication automatique après validation de la PR.

La réussite de cette mission signifie qu'une mise à jour du protocole Hanab Live peut être détectée,
analysée, implémentée et vérifiée sans modification manuelle du code par le mainteneur, tout en
laissant le choix de l'agent remplaçable.
