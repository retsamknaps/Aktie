package aktie.gui.table;

import aktie.gui.CObjListGetter;
import aktie.index.CObjList;

public abstract class CObjListTableContentProvider<E extends CObjListGetter> extends AktieTableContentProvider<CObjList, CObjListGetter>
{

    @Override
    public abstract CObjListGetter[] getElements ( Object a );

    @Override
    public abstract void dispose();

}
