package org.mini.media;

public class MaDataSource extends MaNativeObject {
    MaDataSource next;

    public void setNext(MaDataSource next) {
        if (handle == 0) return;

        this.next = next;//hold for do not gc
        MiniAudio.ma_data_source_set_next(this.handle, next == null ? 0 : next.handle);
    }


}
