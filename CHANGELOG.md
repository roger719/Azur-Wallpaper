# Journal des modifications / Changelog - Azur Wallpaper

Ce document répertorie l'historique complet et exhaustif de toutes les évolutions, nouvelles fonctionnalités, refontes graphiques et correctifs apportés à **Azur Wallpaper** depuis l'introduction du moteur **Spine 2D** (v1.6.0) jusqu'à la version actuelle **2.0.0**.

---

## 📋 Sommaire chronologique

- **[2.0.0]** - *Version Majeure 2.0 : Optimisations, Auto-détection PMA, Performances & Refonte DA*
- **[1.6.5]** - *Animations personnalisées, Localisation complète & Workshop*
- **[1.6.0]** - *Implémentation initiale du moteur Spine 2D*

---

## [2.0.0] - 2026-10-20 (Mise à Jour Majeure 2.0)

### ⚡ Performances & Rendu WebGL (Nouveautés Paramètres)
- **Gestionnaire complet des performances** :
  - **Limite de framerate (FPS)** : Plafonnement configurable de la boucle de rendu Pixi (`30 FPS`, `45 FPS`, `60 FPS` par défaut, `Max 120Hz`) via `app.ticker.maxFPS` pour économiser considérablement la batterie sur écran d'accueil.
  - **Anticrénelage WebGL (Antialiasing)** : Interrupteur permettant d'activer ou de désactiver le multi-sampling WebGL pour soulager la charge GPU.
  - **Résolution de rendu (DPR Scale)** : Sélecteur d'échelle de rendu interne (`100% Native`, `75% Équilibré`, `50% Éco`) ajustant la densité de pixels pour préserver l'autonomie et les performances thermiques.
  - Prise en charge synchronisée en temps réel entre l'écran de prévisualisation interactif et les services de fond d'écran animé (`WaifuSpineWallpaperService` et `WaifuLive2DWallpaperService`).

### 🎨 Rendu Spine & Gestion Universelle de l'Alpha (PMA vs Straight Alpha)
- **Détection Automatique intelligente du mode PMA** :
  - L'application analyse désormais automatiquement la provenance du modèle et la structure de l'archive/atlas.
  - Activation automatique du PMA pour les modèles de **Project QT**, **Nutaku**, **Bikini Girls**, ou dont le fichier `.atlas` déclare `pma: true`.
  - Désactivation automatique du PMA (mode **Straight Alpha**) pour **Arknights**, **Brown Dust 2**, **Azur Lane** et autres modèles standards, éliminant les découpes rectangulaires et saturations de couleurs.
- **Option « Forcer le mode PMA » dans les Paramètres** :
  - Retrait du bouton PMA encombrant de l'écran principal d'animation.
  - Déplacement dans les Paramètres sous le nom clair *« Forcer le mode PMA »*, permettant d'outrepasser manuellement la détection si un modèle non répertorié le nécessite.
- **Rendu en Fond d'Écran d'Accueil (Live Wallpaper)** :
  - Correction du bug où les modèles Spine de Project QT présentaient des contours grisâtres lorsqu'ils étaient appliqués comme fond d'écran réel.
  - Correction des fuites de zoom et de décalage (scale & offset) lors du passage de la prévisualisation au fond d'écran.

### 👆 Comportement Tactile Précis (Touch & Tap)
- **Suppression des animations parasites au toucher** :
  - Correction du comportement où toucher un modèle Spine déclenchait une animation aléatoire même lorsqu'aucune animation n'était souhaitée.
  - Si aucune animation n'est sélectionnée ou si *« Aucune »* (`NONE`) est choisie, le modèle reste serein et poursuit son cycle d'animation au repos sans sursaut.

### 🎭 Direction Artistique & Menu Paramètres (DA Azur / Nothing OS)
- **Harmonisation visuelle complète avec la DA Azur** :
  - Boîtes à coins arrondis (16dp), bordures Nothing OS subtiles (`#B4CDEB`), surfaces lumineuses (`#EDF4FC`), typographie Monospace technique et accents bleu nuit/cobalt (`#0D1B3E`, `#1D4ED8`).
  - Suppression des émojis drapeaux à côté des langues pour une apparence sobre et professionnelle.
  - Épuration des textes redondants (retrait du sous-titre *"Sélectionnez la langue d'affichage"* et du badge de version en double dans le pied de page).
  - Intégration d'un panneau déroulant **Journal des versions (Changelog)** directement consultable dans l'application.

### 🛡️ Conformité Droits d'Auteur & Audio
- **Suppression des médias sous copyright** :
  - Retrait intégral du fichier audio `rickroll.mp3` de l'application et du dépôt.
  - Les services de fond d'écran ne jouent désormais de la musique que si un fichier musical personnalisé valide est explicitement configuré par l'utilisateur.

---

## [1.6.5] - 2026-10-19 / 2026-10-20

### ✨ Animations & Interaction
- **Animation au repos personnalisée (Idle Animation)** :
  - Possibilité de sélectionner n'importe quelle animation disponible dans le modèle pour la jouer en boucle continue au repos, au lieu d'être restreint à l'animation d'origine.
  - Prise en charge uniforme pour les modèles **Spine** et **Live2D**, persistée dans les présets et le fond d'écran.
- **Localisation Intégrale (Français & Anglais)** :
  - Traduction bilingue intégrale de l'application (écrans, dialogues, boutons, formats de date, gestionnaire de fichiers, explorateur Workshop, réglages d'horloge).
  - Basculement de langue dynamique instantané sans redémarrage de l'application.

### 🗂️ Explorateur de Fichiers & Workshop
- **Explorateur Workshop GitHub** :
  - Navigateur intégré pour parcourir, prévisualiser et télécharger des modèles Spine et Live2D directement depuis les dépôts communautaires.
  - Mise en cache locale des skins téléchargés.
- **Explorateur de Fichiers Local** :
  - Navigation arborescente locale dans le stockage de l'appareil.
  - Correction d'un bug d'affichage où seuls les dossiers parents apparaissaient : détection récursive des sous-dossiers et fichiers `model3.json` / `.skel`.
- **Ajustements d'Ergonomie** :
  - Réduction de moitié de la largeur du bouton *« Voir »* dans le Workshop pour un alignement compact.
  - Alignement strict des cartes et boutons avec le champ du nom.
  - Suppression du bouton poubelle dans le Workshop pour éviter les suppressions accidentelles.
  - Centrage dynamique automatique de la caméra de prévisualisation Spine.
  - Compatibilité Samsung OneUI (optimisation `preserveDrawingBuffer` et `WebSettings` contre les écrans noirs).

---

## [1.6.0] - 2026-10-16 (Intégration du Support Spine 2D)

### 🚀 Nouveau Moteur de Rendu Spine 2D
- **Intégration du runtime Spine complet via PixiJS & pixi-spine** :
  - Prise en charge des squelettes binaires `.skel` et JSON `.json` (versions Spine 3.7, 3.8 et 4.x).
  - Décodage des atlas de texture `.atlas` avec textures multi-pages (`.png`, `.webp`, `.jpg`).
  - Découverte dynamique de toutes les animations contenues dans le squelette.
- **Service de Fond d'Écran Animé Dédié** :
  - Création de `WaifuSpineWallpaperService` pour l'exécution fluide et légère de Spine en tâche de fond sur l'écran d'accueil et de verrouillage.
  - Gestion des cycles de vie Android (mise en pause automatique écran éteint pour préserver la batterie, reprise instantanée au déverrouillage).
  - Synchronisation de l'animation de sortie de veille (Wakeup animation) au déverrouillage.
- **Gestionnaire de Modèles & Variantes** :
  - Prise en charge des variantes de skins Spine dans `CharacterModel`.
  - Importation locale d'archives `.zip` contenant des modèles Spine.
