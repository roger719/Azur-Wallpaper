package com.example.waifuengine.data

class AppStrings(val lang: String) {
    val isEn: Boolean get() = lang == "en"

    // Top Header & General
    val appSubtitle: String get() = if (isEn) "AZUR LANE & SPINE WALLPAPERS" else "FONDS D'ÉCRAN AZUR LANE & SPINE"
    val defaultCharacterName: String get() = if (isEn) "Character Name" else "Nom Personnage"

    // Main Tabs & Sections
    val characters: String get() = if (isEn) "CHARACTERS" else "PERSONNAGES"
    val charactersSubtitle: String get() = if (isEn) "Model selection & tree" else "Sélection du modèle & arborescence"
    val files: String get() = if (isEn) "FILES" else "FICHIERS"
    val workshop: String get() = if (isEn) "WORKSHOP" else "WORKSHOP"
    val preview: String get() = if (isEn) "PREVIEW" else "APERÇU"
    val tabAnimation: String get() = if (isEn) "ANIMATION" else "ANIMATION"
    val tabTransform: String get() = if (isEn) "SCALE & POSITION" else "ÉCHELLE & POSITION"
    val tabBackground: String get() = if (isEn) "BACKGROUND & EFFECTS" else "ARRIÈRE-PLAN & EFFETS"
    val tabClock: String get() = if (isEn) "CLOCK & DATE" else "HORLOGE & DATE"
    val tabAudio: String get() = if (isEn) "AUDIO & INTERACTIONS" else "SON & INTERACTIONS"
    val setAsWallpaper: String get() = if (isEn) "SET AS WALLPAPER" else "DÉFINIR COMME FOND D'ÉCRAN"
    val restartService: String get() = if (isEn) "RESTART SERVICE" else "REDÉMARRER LE SERVICE"

    // Settings Dialog
    val settings: String get() = if (isEn) "SETTINGS" else "PARAMÈTRES"
    val settingsSubtitle: String get() = if (isEn) "General preferences" else "Configuration générale"
    val languageSection: String get() = if (isEn) "LANGUAGE" else "LANGUE"
    val languageDescription: String get() = if (isEn) "Select interface display language" else "Sélectionnez la langue d'affichage"
    val french: String get() = "Français"
    val english: String get() = "English"
    val appVersion: String get() = "Version 1.6.5"
    val close: String get() = if (isEn) "FERMER" else "FERMER"
    val cancel: String get() = if (isEn) "Annuler" else "Annuler"

    // Files Dialog
    val fileExplorer: String get() = if (isEn) "FILE EXPLORER" else "EXPLORATEUR DE FICHIERS"
    val localTree: String get() = if (isEn) "Local storage tree" else "Arborescence locale"
    val importModel: String get() = if (isEn) "+ IMPORT" else "+ IMPORTER"
    val noCharacterImported: String get() = if (isEn) "No characters imported yet" else "Aucun personnage importé"
    val delete: String get() = if (isEn) "SUPPRIMER" else "SUPPRIMER"
    val apply: String get() = if (isEn) "APPLIQUER" else "APPLIQUER"

    // Workshop Dialog
    val workshopTitle: String get() = if (isEn) "GITHUB WORKSHOP" else "WORKSHOP GITHUB"
    val workshopSubtitle: String get() = if (isEn) "Community Spine & Live2D repository" else "Dépôt communautaire Spine & Live2D"
    val searchPlaceholder: String get() = if (isEn) "Rechercher..." else "Rechercher..."
    val allFilter: String get() = if (isEn) "Tous" else "Tous"
    val skinsCount: String get() = if (isEn) "skins" else "skins"
    val viewDown: String get() = if (isEn) "Voir ↓" else "Voir ↓"
    val collapseUp: String get() = if (isEn) "Replier ↑" else "Replier ↑"
    val btnPreview: String get() = if (isEn) "Aperçu" else "Aperçu"
    val btnDownload: String get() = if (isEn) "Télécharger" else "Télécharger"
    val btnApply: String get() = if (isEn) "Appliquer" else "Appliquer"
    val btnKeep: String get() = if (isEn) "GARDER" else "GARDER"
    val previewTitle: String get() = if (isEn) "CHARACTER PREVIEW" else "APERÇU DU PERSONNAGE"
    val previewSubtitle: String get() = if (isEn) "Interactive inspection" else "Inspection interactive"
    val repoAndAccessKey: String get() = if (isEn) "REPO & ACCESS KEY" else "DÉPÔT & CLÉ D'ACCÈS"
    val repoUrlLabel: String get() = if (isEn) "GITHUB REPOSITORY URL" else "URL DU DÉPÔT GITHUB"
    val accessKeyLabel: String get() = if (isEn) "ACCESS KEY / PERSONAL TOKEN (PAT)" else "CLÉ D'ACCÈS / TOKEN PERSONNEL (PAT)"
    val tokenSaved: String get() = if (isEn) "● Access key configured" else "● Clé d'accès renseignée"
    val tokenPublicOnly: String get() = if (isEn) "○ Public repository only (no key)" else "○ Dépôt public uniquement (aucune clé)"
    val tokenTip: String get() = if (isEn) "Tip: For private repositories, use a token with 'repo' or 'contents:read' access." else "Astuce : Pour un dépôt privé GitHub, utilisez un Token avec accès 'repo' ou 'contents:read'."
    val saveAndLoad: String get() = if (isEn) "ENREGISTRER & CHARGER" else "ENREGISTRER & CHARGER"
    val exploringRepo: String get() = if (isEn) "Exploring GitHub repository..." else "Exploration du dépôt GitHub..."
    val sortingModels: String get() = if (isEn) "Classifying models by game..." else "Classement des modèles par jeu en cours"
    val loadFailed: String get() = if (isEn) "Loading failed" else "Échec du chargement"
    val retry: String get() = if (isEn) "RÉESSAYER" else "RÉESSAYER"
    val noCharacterFound: String get() = if (isEn) "No matching character found." else "Aucun personnage correspondant trouvé."
    val installedStatus: String get() = if (isEn) "installed" else "installé(s)"

    companion object {
        fun get(prefs: WaifuPreferences): AppStrings = AppStrings(prefs.appLanguage)
    }
}
