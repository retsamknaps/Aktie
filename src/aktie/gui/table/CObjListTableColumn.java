package aktie.gui.table;

import aktie.gui.CObjListGetter;
import aktie.index.CObjList;

public class CObjListTableColumn<E extends CObjListGetter> extends AktieTableColumn<CObjList, CObjListGetter>
{

    public CObjListTableColumn ( CObjListTable<E> parent, int style )
    {
        super ( parent, style );
    }

}
