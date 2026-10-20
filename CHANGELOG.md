# Journal des modifications / Changelog - Azur Wallpaper

Ce document répertorie l'ensemble des évolutions, nouvelles fonctionnalités et correctifs apportés à l'application **Azur Wallpaper** depuis l'intégration du moteur **Spine 2D** jusqu'à la version majeure **2.0**.

---

## [2.0.0] - 2026-10-20 (Mise à jour Majeure 2.0)

### 🎨 Rendu Spine & Gestion Universelle de l'Alpha (PMA vs Straight Alpha)
- **Support hybride Premultiplied Alpha & Straight Alpha** :
  - **Arknights & Brown Dust 2** : Résolution complète des distorsions, coupures rectangulaires et saturations de textures dues au mode PMA forcé. Les atlas Straight Alpha s'affichent désormais avec une netteté absolue sans artéfacts.
  - **Project QT** : Maintien du traitement adapté pour les atlas Spine 3.7 exportés en Premultiplied Alpha sans mention `pma: true` dans le fichier `.atlas` (suppression des liserés grisâtres et restauration de la translucidité limpide).
- **Option & Toggle PMA dédié** :
  - Ajout du contrôle interactif *« Mode Alpha PMA (Premultiplié) »* dans la section *Animations & Interaction*.
  - Détection automatique intelligente lors de la sélection d'un personnage (activé par défaut pour Project QT, désactivé pour Arknights / Brown Dust 2).
  - Basculement instantané en temps réel dans la prévisualisation et persistance au sein des **Présets** utilisateur (`isSpinePma`).

### 👆 Correction des Interactions Tactiles (Tap / Clic)
- **Suppression des animations aléatoires involontaires** :
  - Résolution du problème où toucher un modèle Spine déclenchait une animation aléatoire lorsqu'aucune animation de toucher n'était configurée.
  - Désormais, si la configuration de toucher est vide ou sur *« Aucune »*, le modèle reste parfaitement immobile et conserve son animation au repos.
  - Prise en charge uniforme en prévisualisation interactive et sur le fond d'écran animé actif (`WaifuSpineWallpaperService`).

### 📱 Refonte Visuelle du Menu Paramètres (DA Azur)
- **Harmonisation complète avec la Direction Artistique (DA) Azur** :
  - Remplacement de l'ancien thème gris neutre par la palette officielle Azur / Nothing OS :
    - Surfaces bleu céruléen très lumineux (`#EDF4FC`).
    - Bordures cobalt subtiles (`#B4CDEB`).
    - Typographie et boutons bleu nuit royal profond (`#0D1B3E`, `#0B1B3D`).
    - Accents bleu vif (`#1D4ED8`).
  - Tuiles de sélection de langue (Français / Anglais) redessinées avec badges d'état actifs.
  - Intégration d'un visualiseur de Changelog accessible directement depuis les Paramètres.

---

## [1.6.5] - 2026-10-19 / 2026-10-20

### ✨ Animations & Interaction
- **Animation au repos personnalisée (Idle Animation)** :
  - Possibilité de choisir n'importe quelle motion du modèle pour tourner en boucle permanente en tant qu'animation de repos, ou de revenir à l'animation par défaut.
  - Prise en charge complète pour les deux moteurs **Spine** et **Live2D**, synchronisée avec les fonds d'écran animés et les présets.
- **Localisation Intégrale (Français & Anglais)** :
  - Traduction bilingue exhaustive de tous les écrans, réglages d'horloge, formats de date, gestionnaire de fichiers, explorateur Workshop et messages toast.
  - Sélecteur de langue dynamique instantané sans redémarrage requis.
- **Ergonomie & Épuration** :
  - Retrait des emojis drapeaux dans le menu Paramètres pour un rendu sobre et professionnel.
  - Correction de l'arborescence des dossiers Live2D dans l'explorateur de fichiers (détection et ouverture directes des modèles `.model3.json`).
  - Centrage dynamique du conteneur de prévisualisation Spine.
  - Alignement strict des cartes et boutons du Workshop.

---

## [1.6.0] - 2026-10-16 (Intégration du Support Spine 2D)

### 🚀 Nouveau Moteur de Rendu Spine 2D
- **Intégration du runtime Spine complet via PixiJS & pixi-spine** :
  - Prise en charge des squelettes binaires `.skel` et JSON `.json` (versions Spine 3.7, 3.8 et 4.x).
  - Prise en charge des fichiers `.atlas` et des textures multi-pages (`.png`, `.webp`, `.jpg`).
- **Fond d'Écran Animé Dédié** :
  - Création du service `WaifuSpineWallpaperService` exécutant le rendu Spine en tâche de fond avec support multi-écrans virtuels et gestion des cycles de veille / déverrouillage.
- **Gestionnaire de Modèles & Workshop** :
  - Compatibilité avec l'importation locale d'archives `.zip` contenant des modèles Spine.
  - Détection automatique des sous-skins et des variantes.
  - Découverte des animations Spine et interconnexion avec les déclencheurs de sortie de veille et de toucher.
