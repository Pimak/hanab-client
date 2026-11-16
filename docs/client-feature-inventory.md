# Inventaire fonctionnel du client Hanab

## Objectif

Ce document recense les capacités qu'une bibliothèque cliente JVM doit exposer pour
interagir avec Hanab Live. Il sert de base à la roadmap du module `hanab-client`.

Le client est une bibliothèque réutilisable, pas une copie de l'interface web. Il doit
fournir des modèles immuables, des opérations métier et des événements typés. Le rendu
visuel, les raccourcis clavier, les animations et le stockage local restent à la charge
de l'application qui consomme la bibliothèque.

Les priorités proposées sont :

- **P0** : nécessaire pour une première utilisation fiable du client ;
- **P1** : nécessaire pour couvrir les parcours Hanab Live courants ;
- **P2** : capacité avancée, confort ou parité complète ;
- **À confirmer** : dépend d'un besoin du consommateur ou d'une décision de périmètre.

## Principes de conception

- Masquer le format `commande JSON` et le transport WebSocket derrière une API métier.
- Séparer l'état courant, les événements reçus et les actions envoyées.
- Préserver les informations inconnues ou ajoutées par le serveur pour rester compatible
  avec les évolutions du protocole.
- Rendre explicites les erreurs de transport, d'authentification, de validation et de
  permission.
- Permettre la reconnexion sans perdre l'identité de session ni mélanger deux parties.
- Ne pas déduire les règles de jeu côté client : le serveur reste l'autorité.

## Périmètre fonctionnel

### 1. Accès, identité et session

| ID | Priorité | Fonctionnalité | Attendu côté client |
| --- | --- | --- | --- |
| AUTH-01 | P0 | Connexion par identifiant et mot de passe | Appel HTTP de connexion, conservation sûre de la session et retour d'une erreur typée. |
| AUTH-02 | P1 | Identité courante | Récupérer l'utilisateur connecté, son statut, ses relations et les informations de session. |
| AUTH-03 | P1 | Déconnexion | Fermer proprement la connexion et invalider les données de session locales. |
| AUTH-04 | P1 | Création de compte invité ou bot | Supporter les paramètres de connexion utilisés par les clients non web, si ce parcours est retenu. |
| AUTH-05 | P2 | Réinitialisation de mot de passe | Exposer l'appel HTTP correspondant sans stocker le mot de passe ou le secret en clair. |
| AUTH-06 | P1 | Version et compatibilité | Lire la version du serveur/client et détecter un protocole incompatible. |

### 2. Transport et cycle de vie

| ID | Priorité | Fonctionnalité | Attendu côté client |
| --- | --- | --- | --- |
| TRANS-01 | P0 | Connexion WebSocket | Établir la session avec les paramètres adaptés au type de client. |
| TRANS-02 | P0 | Encodage/décodage du protocole | Gérer le préfixe de commande, le JSON, les identifiants de corrélation et les réponses asynchrones. |
| TRANS-03 | P0 | Événements serveur | Distribuer les messages sans imposer un framework réactif ou une UI. |
| TRANS-04 | P0 | Fermeture et erreurs | Distinguer fermeture volontaire, refus serveur, timeout, message invalide et erreur réseau. |
| TRANS-05 | P1 | Reconnexion | Reconnecter avec backoff, rétablir le contexte si possible et signaler les changements d'état. |
| TRANS-06 | P1 | Heartbeat et watchdog | Détecter une connexion silencieuse ou un handshake bloqué. |
| TRANS-07 | P1 | Annulation et timeout | Annuler une opération sans interrompre les autres demandes sur la session. |
| TRANS-08 | P2 | Observabilité | Exposer des métriques et journaux sûrs, sans secret ni payload privé. |

### 3. Lobby et présence

| ID | Priorité | Fonctionnalité | Attendu côté client |
| --- | --- | --- | --- |
| LOBBY-01 | P0 | Chargement du lobby | Recevoir l'état initial, les tables, le nombre de parties et l'état de chargement. |
| LOBBY-02 | P0 | Liste des utilisateurs en ligne | Suivre les arrivées, départs, activités et statuts des utilisateurs. |
| LOBBY-03 | P1 | Liste des tables | Modéliser les tables en attente, en jeu, en replay partagé et leurs spectateurs. |
| LOBBY-04 | P1 | Mise à jour temps réel | Appliquer les événements différentiels sans perdre l'ordre ni dupliquer l'état. |
| LOBBY-05 | P1 | Historique et meilleurs scores | Charger les pages d'historique et les statistiques associées à un utilisateur ou une variante. |
| LOBBY-06 | P2 | Sons et annonces du lobby | Recevoir les notifications et sons diffusés par le serveur ; leur lecture reste externe. |

### 4. Tables et préparation d'une partie

| ID | Priorité | Fonctionnalité | Attendu côté client |
| --- | --- | --- | --- |
| TABLE-01 | P0 | Créer une table | Définir le nom, le nombre de joueurs, la variante, les options et le mot de passe éventuel. |
| TABLE-02 | P0 | Rejoindre et quitter une table | Gérer les droits, les erreurs de table pleine/fermée et le retour à l'état du lobby. |
| TABLE-03 | P1 | Specter une table | Rejoindre une table en lecture seule, y compris avant le début de la partie. |
| TABLE-04 | P0 | Démarrer une partie | Autoriser le propriétaire à lancer la partie et refléter la transition d'état. |
| TABLE-05 | P1 | Gérer le propriétaire/leader | Changer le propriétaire, transmettre le leadership et modéliser les permissions. |
| TABLE-06 | P1 | Expulser un joueur | Exposer l'action réservée au propriétaire avec retour d'erreur explicite. |
| TABLE-07 | P1 | Modifier les options avant démarrage | Mettre à jour la variante, le rythme, les règles spéciales et les réglages de table. |
| TABLE-08 | P1 | Rejouer ou refaire une partie | Redémarrer avec les mêmes joueurs ou recréer une table depuis la partie terminée. |
| TABLE-09 | P2 | Diviser une table | Supporter la création de nouvelles tables à partir d'un groupe de joueurs. |

### 5. Partie en direct

| ID | Priorité | Fonctionnalité | Attendu côté client |
| --- | --- | --- | --- |
| GAME-01 | P0 | État initial de la partie | Recevoir le paquet, les mains vues selon la perspective, les piles, la pioche, les défausses, les jetons et les tours. |
| GAME-02 | P0 | Jouer une carte | Envoyer une action validée par le serveur avec l'ordre de carte et la perspective correcte. |
| GAME-03 | P0 | Défausser une carte | Envoyer l'action et appliquer l'événement de remplacement de carte. |
| GAME-04 | P0 | Donner un indice | Gérer les indices de couleur et de rang, leur cible et les cartes touchées. |
| GAME-05 | P0 | Événements de tour | Consommer les événements de jeu dans l'ordre : pioche, indice, jeu, défausse, faute et fin. |
| GAME-06 | P0 | Fin de partie | Modéliser les conditions de fin, le score, les vies/frappes, le temps et le résultat final. |
| GAME-07 | P1 | Notes sur les cartes | Ajouter, modifier et supprimer les notes personnelles et les notes visibles selon les droits. |
| GAME-08 | P1 | Perspective joueur/spectateur | Gérer la vue d'un joueur, du spectateur, du joueur observé et les restrictions d'information. |
| GAME-09 | P1 | Spectateurs | Suivre les connexions, départs, noms et vues des spectateurs. |
| GAME-10 | P1 | Pause et reprise | Demander une pause, reprendre et refléter l'état de pause partagé. |
| GAME-11 | P1 | Vote de fin/arrêt | Voter pour terminer, terminer immédiatement selon les permissions et suivre les votes. |
| GAME-12 | P1 | Parties chronométrées | Lire les horloges, les ajouts de temps, l'expiration et les pauses planifiées. |
| GAME-13 | P2 | Speedrun | Exposer le mode speedrun et ses contrôles/action semantics spécifiques. |
| GAME-14 | P2 | Signaux sonores de partie | Recevoir les effets diffusés à tous ou à un groupe ; la lecture audio reste externe. |

### 6. Règles, variantes et catalogue

| ID | Priorité | Fonctionnalité | Attendu côté client |
| --- | --- | --- | --- |
| RULES-01 | P0 | Catalogue des variantes | Charger les noms, identifiants, couleurs et paramètres publiés par le serveur. |
| RULES-02 | P0 | Options de partie | Modéliser les options génériques : nombre de cartes, indices vides, pioche aveugle, tout ou rien, etc. |
| RULES-03 | P1 | Variantes multi-couleurs | Préserver les identités de cartes, couleurs, mélanges et combinaisons spéciales sans les coder en dur dans l'API publique. |
| RULES-04 | P1 | Assignations de personnages | Modéliser les contraintes et identités de personnages lorsque la variante les active. |
| RULES-05 | P1 | Métadonnées de score | Exposer le score maximal et les informations nécessaires au suivi des variantes manquantes. |
| RULES-06 | P2 | Validation locale non autoritaire | Fournir des aides de validation ou de présentation sans contredire la décision du serveur. |

### 7. Replay et analyse

| ID | Priorité | Fonctionnalité | Attendu côté client |
| --- | --- | --- | --- |
| REPLAY-01 | P0 | Charger un replay sauvegardé | Charger une partie par identifiant et obtenir ses métadonnées/actions. |
| REPLAY-02 | P0 | Naviguer par tour | Reconstituer l'état à un tour précis, avancer, reculer et aller au début/à la fin. |
| REPLAY-03 | P1 | Replay sans spoiler | Exposer une vue limitée à l'information connue au tour sélectionné. |
| REPLAY-04 | P1 | Replay JSON | Importer et exporter le format JSON Hanab Live avec validation et erreurs localisées. |
| REPLAY-05 | P1 | Replay partagé | Rejoindre une session, suivre le leader, se désynchroniser volontairement et se resynchroniser. |
| REPLAY-06 | P1 | Leader de replay | Transférer le leadership et diffuser le tour, les flèches, les sons et les suggestions. |
| REPLAY-07 | P1 | Annotations de replay | Ajouter des flèches, notes, tags et suggestions de tour. |
| REPLAY-08 | P2 | Hypothétiques | Démarrer/terminer une hypothèse, passer, revenir en arrière, afficher les cartes piochées et modifier une carte. |
| REPLAY-09 | P2 | Rejouer depuis un état | Créer une partie partielle ou un scénario à partir d'un replay existant. |
| REPLAY-10 | P2 | Temps de revue | Exposer le temps passé en replay si le serveur le comptabilise. |

### 8. Chat et relations sociales

| ID | Priorité | Fonctionnalité | Attendu côté client |
| --- | --- | --- | --- |
| SOCIAL-01 | P0 | Chat de lobby/table/partie/replay | Recevoir et envoyer les messages dans le bon contexte. |
| SOCIAL-02 | P1 | Messages privés | Envoyer, recevoir et charger l'historique d'une conversation privée. |
| SOCIAL-03 | P1 | Indicateur de saisie | Publier et consommer les événements de saisie sans les persister comme messages. |
| SOCIAL-04 | P1 | Commandes de chat | Exposer une couche de commandes typée pour les opérations utiles : pause, replay, tag, leader, variante, etc. |
| SOCIAL-05 | P1 | Amis | Ajouter, supprimer et lister les amis. |
| SOCIAL-06 | P1 | Comptes liés | Lier, délier et lister des comptes associés. |
| SOCIAL-07 | P1 | Blocage | Bloquer, débloquer et lister les utilisateurs bloqués. |
| SOCIAL-08 | P2 | Sons de chat | Recevoir les sons et annonces associés aux messages ; ne pas imposer un moteur audio. |

### 9. Tags, recherche et intégrations

| ID | Priorité | Fonctionnalité | Attendu côté client |
| --- | --- | --- | --- |
| INT-01 | P1 | Taguer une partie | Ajouter, retirer et lister les tags d'une partie ou d'un replay. |
| INT-02 | P1 | Rechercher par tag | Rechercher les parties associées à un tag et paginer les résultats. |
| INT-03 | P1 | Liens partageables | Générer et analyser les références de table, replay, replay partagé et tour. |
| INT-04 | P1 | Bot/app client | Supporter les paramètres de connexion et les limitations propres aux bots ou applications. |
| INT-05 | P2 | Seed et parties similaires | Rechercher des parties par seed, score ou variante pour l'entraînement et la compétition. |
| INT-06 | P2 | Compatibilité API HTTP | Encapsuler les endpoints HTTP publics non couverts par WebSocket, avec pagination et erreurs. |

## Contrat technique à respecter

Le dépôt officiel décrit le wire protocol comme `commande JSON` sur WebSocket et comme
des corps JSON pour HTTP. Le client Java devra donc prévoir :

- un codec qui sépare le nom de commande du payload ;
- des DTO de transport distincts des modèles publics du domaine ;
- le rejet ou la conservation contrôlée des champs inconnus selon la politique de compatibilité ;
- l'omission des champs optionnels absents, sans transformer implicitement un `null` en valeur ;
- la validation des bornes numériques et des timestamps RFC 3339 UTC ;
- la gestion des unions discriminées du protocole (`type`, état, mode de vue, action) ;
- la protection contre la fuite d'identités de cartes qui ne sont pas visibles dans la perspective courante ;
- des fixtures de compatibilité pour chaque règle de sérialisation et de désérialisation.

Les commandes à couvrir en premier sont `tableCreate`, `tableJoin`, `tableUpdate`,
`tableStart`, `tableSpectate`, `playerAction`, `pause`, `note`, `replayCreate`,
`replayAction`, `chat` et `chatPm`. Les commandes de relations (`friend`, `link`,
`block`), d'historique (`gameHistory`, `seedGames`, `tagSearch`) et de configuration
complètent ensuite la surface publique.

## Fonctionnalités volontairement hors du cœur

Ces éléments peuvent être fournis par une application au-dessus de la bibliothèque :

- rendu des cartes, flèches, pips, bordures et indicateurs de carte critique ;
- calcul purement visuel de l'empathie, du mode daltonien, du thème et de l'orientation ;
- raccourcis clavier, glisser-déposer, pré-jouage et animations ;
- lecture des sons et notifications desktop ;
- persistance locale des préférences et cache UI ;
- navigation entre pages et affichage des profils.

La bibliothèque doit toutefois exposer les données nécessaires à ces fonctions, notamment
les événements de cartes, les identités visibles, les notes, les temps, les perspectives,
les métadonnées de replay et les permissions.

## Roadmap proposée

### Phase 0 — Contrat et fondations (P0)

1. Figer les modèles `User`, `Session`, `Table`, `GameState`, `GameEvent`, `ReplayGame`
   et les erreurs publiques.
2. Ajouter les fixtures de messages du protocole et les tests de compatibilité.
3. Implémenter HTTP login/identity et le transport WebSocket.
4. Implémenter corrélation des requêtes, timeouts, fermeture et reconnexion de base.

### Phase 1 — Parcours lobby et partie standard (P0)

1. Charger le lobby et les utilisateurs.
2. Créer, rejoindre, quitter et démarrer une table.
3. Recevoir l'état initial et les événements d'une partie.
4. Envoyer jouer, défausser et donner un indice.
5. Gérer fin de partie, erreurs, pause et reconnexion.

### Phase 2 — Options de partie et collaboration (P1)

1. Ajouter le catalogue des variantes et options.
2. Ajouter spectateurs, notes, perspectives et statistiques de partie.
3. Ajouter chat, messages privés, amis et blocage.
4. Ajouter parties chronométrées, restart/remake et changement de leader.

### Phase 3 — Replays et partage (P1)

1. Charger et parcourir les replays sauvegardés.
2. Implémenter spoiler-free et import/export JSON.
3. Implémenter replay partagé, leader, synchronisation, suggestions et annotations.
4. Ajouter tags, recherche et liens partageables.

### Phase 4 — Parité avancée (P2)

1. Hypothétiques et modification de cartes.
2. Speedruns, seeds et parties similaires.
3. Support complet des commandes et effets sonores.
4. Hardening de compatibilité : messages inconnus, versionnement, métriques et tests de charge.

### Mission suivante — Synchronisation continue avec le serveur courant (P0)

Automatiser la détection des changements de Hanab Live, l'adaptation de la librairie par un agent
interchangeable, le lancement d'un serveur local éphémère et les tests d'intégration contre ce
serveur. Chaque synchronisation doit produire un changelog et une PR vérifiable avant la
publication de la version correspondant au serveur courant. Voir
[`continuous-server-synchronization.md`](continuous-server-synchronization.md).

## Critères de sortie de la première version utilisable

- Un appelant peut se connecter, se reconnecter et fermer la session proprement.
- Un appelant peut créer ou rejoindre une table, démarrer une partie et suivre son état.
- Un appelant peut jouer, défausser et donner un indice sans manipuler de JSON brut.
- Les événements serveur sont livrés dans l'ordre et les erreurs sont actionnables.
- Une partie terminée peut être chargée et parcourue comme replay.
- Les tests couvrent au minimum login, handshake, reconnexion, table, action de jeu,
  fin de partie et replay.
- L'API ne dépend ni de Spring, ni de Quarkus, ni de MCP, ni d'une UI.

## Sources de référence

Les fonctionnalités observées sont issues des sources de première partie suivantes :

- [Manuel et liste des fonctionnalités Hanab Live](https://github.com/Hanabi-Live/hanabi-live/blob/main/docs/features.md)
- [Variantes Hanab Live](https://github.com/Hanabi-Live/hanabi-live/blob/main/docs/variants.md)
- [Commandes de chat](https://github.com/Hanabi-Live/hanabi-live/blob/main/docs/chat-commands.md)
- [Règles du jeu](https://github.com/Hanabi-Live/hanabi-live/blob/main/docs/rules.md)
- [Protocole des requêtes](https://github.com/Hanabi-Live/hanabi-live/blob/main/server/protocol/src/requests.rs)
- [Protocole du lobby](https://github.com/Hanabi-Live/hanabi-live/blob/main/server/protocol/src/lobby.rs)
- [Protocole des parties](https://github.com/Hanabi-Live/hanabi-live/blob/main/server/protocol/src/game.rs)
- [Protocole des replays](https://github.com/Hanabi-Live/hanabi-live/blob/main/server/protocol/src/replay.rs)
- [Protocole HTTP](https://github.com/Hanabi-Live/hanabi-live/blob/main/server/protocol/src/http.rs)
- [Décisions d'architecture de `hanab-client`](architecture.md)

Les priorités et le découpage en phases sont des propositions pour ce module. Ils devront
être confirmés avec les consommateurs avant de devenir un contrat public.

## Décisions à prendre avant l'implémentation

- Quelle version du protocole Hanab Live doit être supportée et quelle stratégie de
  compatibilité est attendue lors d'un changement serveur ?
- La première version doit-elle couvrir un joueur, un spectateur, un replay, un bot,
  ou les quatre parcours ?
- L'authentification doit-elle gérer uniquement le login/mot de passe ou aussi les
  cookies, tokens d'identité et création de compte bot ?
- Les variantes doivent-elles rester génériques dans Java, ou le module doit-il exposer
  des règles métier détaillées ?
- Les notes et conventions doivent-elles être modélisées sémantiquement ou transportées
  comme des données opaques ?
- Les hypothétiques et l'édition de cartes sont-elles nécessaires au premier release ?
- Quel comportement est attendu après remplacement de session ou perte de connexion ?
