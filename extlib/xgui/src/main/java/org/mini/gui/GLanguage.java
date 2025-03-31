/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.gui;

import org.mini.apploader.bean.LangBean;
import org.mini.json.JsonParser;

import java.util.HashMap;
import java.util.Map;

/**
 * 使用豆包进行翻译，提示语如下：
 * 按这些语言代码翻译短语 “UPDATE”  ,翻译顺序为    "en_US", "zh_CN",  "zh_TW",  "ko_KR",  "fr_FR",  "es_ES",  "it_IT",  "ja_JP",  "de_DE", "ru_RU"，输出的格式为用半角双引号把翻译出的文字括起来，并用半角逗号分隔，不要加入其他解释在答案里
 *
 * @author Gust
 */
public class GLanguage {

    static public final int ID_NO_DEF = -1;
    static public final int ID_ENG = 0;
    static public final int ID_CHN = 1;
    static public final int ID_CHT = 2;
    static public final int ID_KOR = 3;
    static public final int ID_FRA = 4;
    static public final int ID_ESP = 5;
    static public final int ID_ITA = 6;
    static public final int ID_JPA = 7;
    static public final int ID_GER = 8;
    static public final int ID_RUS = 9;

    static public final String[] LANG_NAMES = {
            "English",//english
            "简体中文",//simple chinese
            "繁體中文",//traditional chinese
            "한국어",//korean
            "Français",//frances
            "Español",//spanish
            "Italiano",//italiano
            "日本語",//japanese
            "Deutsch",//german
            "Русский"//russian
    };
    static public final String[] LANG_CODES = {
            "en_US",
            "zh_CN",
            "zh_TW",
            "ko_KR",
            "fr_FR",
            "es_ES",
            "it_IT",
            "ja_JP",
            "de_DE",
            "ru_RU"
    };
    static public final String[] LANG_CODES_SHORT = {
            "en",
            "zh",
            "zh",
            "ko",
            "fr",
            "es",
            "it",
            "ja",
            "de",
            "ru"
    };
    static Map<String, String[]> lang = new HashMap();
    static Map<String, Map<String, String[]>> app2ext = new HashMap();

    static int cur_lang = ID_ENG;

    /**
     * api range language strings
     */
    static {
        initGuiSrings();
    }

    static private void initGuiSrings() {
        addStringInner("SeleAll", new String[]{"SeleAll", "全选", "全選", "모두선택", "Tout sélectionner", "Seleccionar todo", "Seleziona tutto", "すべて選択", "Alles auswählen", "Выбрать все"});
        addStringInner("Copy", new String[]{"Copy", "复制", "復制", "복사", "Copier", "Copiar", "Copiare", "コピー", "Kopieren", "Копировать"});
        addStringInner("Select", new String[]{"Select", "选择", "選擇", "선택", "Sélectionner", "Seleccionar", "Selezionare", "選択", "Auswählen", "Выбрать"});
        addStringInner("Paste", new String[]{"Paste", "粘贴", "貼上", "붙여넣기", "Coller", "Pegar", "Incollare", "貼り付ける", "Einfügen", "Вставить"});
        addStringInner("Cut", new String[]{"Cut", "剪切", "剪下", "자르기", "Couper", "Cortar", "Tagliare", "切り取る", "Schneiden", "Вырезать"});
        addStringInner("Perform", new String[]{"Perform", "执行", "執行", "수행하다", "Effectuer", "Realizar", "Eseguire", "実行する", "Durchführen", "Выполнять"});
        addStringInner("Cancel", new String[]{"Cancel", "取消", "取消", "취소하다", "Annuler", "Cancelar", "Annullare", "キャンセル", "Abbrechen", "Отменить"});
        addStringInner("Ok", new String[]{"Ok", "确定", "確認", "확인하다", "Confirmer", "Confirmar", "Confermare", "確認する", "Bestätigen", "Подтвердить"});
        addStringInner("Save to album", new String[]{"Save to album", "存入相册", "存入相冊", "앨범에 저장하기", "Enregistrer dans l'album", "Guardar en el álbum", "Salva nell'album", "アルバムに保存する", "In das Album speichern", "Сохранить в альбоме"});
        addStringInner("Message", new String[]{"Message", "消息", "訊息", "메시지", "Message", "Mensaje", "Messaggio", "メッセージ", "Nachricht", "Сообщение"});
        addStringInner("Do you sure delete :", new String[]{"Do you sure delete: ", "你要删除文件或目录： ", "你要刪除檔案或目錄： ", "파일 또는 디렉토리를 삭제하려고 합니다: ", "Vous êtes sur le point de supprimer le fichier ou le répertoire :", "Estás a punto de eliminar el archivo o directorio:", "Stai per eliminare il file o la directory:", "あなたはファイルまたはディレクトリを削除しようとしています: ", "Sie sind im Begriff, die Datei oder das Verzeichnis zu löschen:", "Вы собираетесь удалить файл или каталог: "});
        addStringInner("Create new folder :", new String[]{"Name for the new folder to be created:", "创建新文件夹名称： ", "建立新資料夾名稱： ", "새 폴더의 이름: ", "Nom du nouveau dossier à créer :", "Nombre de la nueva carpeta a crear:", "Nome per la nuova cartella da creare:", "作成する新しいフォルダの名前:", "Name für den neu zu erstellenden Ordner:", "Имя нового создаваемого каталога:"});
        addStringInner("Folder Name", new String[]{"Folder Name", "文件夹名称", "資料夾名稱", "폴더 이름", "Nom du dossier", "Nombre de la carpeta", "Nome della cartella", "フォルダ名", "Ordnername", "Имя папки"});
        addStringInner("Delete", new String[]{"Delete", "删除", "刪除", "삭제", "Supprimer", "Eliminar", "Eliminare", "削除", "Löschen", "Удалить"});
    }

    static public int getSupportedLang() {
        return LANG_NAMES.length;
    }

    static public void setCurLang(int langType) {
        cur_lang = langType;
    }

    static public int getCurLang() {
        return cur_lang;
    }

    static public String getString(String appId, String key) {
        return getString(appId, key, cur_lang);
    }

    static private void addStringInner(String key, String[] values) {
        if (key != null && values != null) {
            lang.put(key, values);
        }
    }

    static public void addString(String appId, String key, String[] values) {
        if (appId != null && key != null && values != null) {
            Map<String, String[]> ext = app2ext.get(appId);
            if (ext == null) {
                ext = new HashMap();
                app2ext.put(appId, ext);
            }
            ext.put(key, values);
        }
    }

    static public String getString(String appId, String key, int langType) {
        String[] ss = null;
        Map<String, String[]> ext = app2ext.get(appId);
        if (ext != null) {
            ss = ext.get(key);
        }
        if (ss == null) {
            ss = lang.get(key);
        }
        if (ss == null) {
            return key;
        }
        if (langType < 0 || langType >= ss.length) {
            return ss[ID_ENG];
        }
        return ss[langType];
    }

    static public void clear(String appId) {
        Map<String, String[]> ext = app2ext.get(appId);
        if (ext != null) {
            app2ext.remove(appId);
        }
    }

    public static String getLangCode(int lang) {
        if (lang >= 0 && lang < LANG_CODES.length) {
            return LANG_CODES[lang];
        } else {
            return LANG_CODES[ID_ENG];
        }
    }

    public static String getLangName(int lang) {
        if (lang >= 0 && lang < LANG_NAMES.length) {
            return LANG_NAMES[lang];
        } else {
            return LANG_NAMES[ID_ENG];
        }
    }

    public static int getIdByShortName(String sysLang) {
        int lang = ID_ENG;
        for (int i = 0; i < LANG_CODES_SHORT.length; i++) {
            if (sysLang.equalsIgnoreCase(LANG_CODES_SHORT[i])) {
                lang = i;
                break;
            }
        }
        return lang;
    }


    /**
     * register String resources
     * json format:
     * <pre>
     * {
     *   "lang": {
     *     "STR_Save": [
     *       "Save",
     *       "退出",
     *       "退出"
     *     ],
     *     "STR_Browse": [
     *       "Browse",
     *       "浏览",
     *       "瀏覽"
     *     ]
     *   }
     * }
     * </pre>
     *
     * @param appId
     * @param jsonStr
     */
    public static void regJsonStrings(String appId, String jsonStr) {

        JsonParser<LangBean> parser = new JsonParser<>();
        LangBean langBean = parser.deserial(jsonStr, LangBean.class);

        for (String key : langBean.getLang().keySet()) {
            GLanguage.addString(appId, key, langBean.getLang().get(key));
        }
    }

}
