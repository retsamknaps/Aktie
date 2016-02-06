package aktie.gui;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import aktie.data.CObj;

public class CObjContentProvider implements IStructuredContentProvider
{

    private List<CObjElement> rowList;

    public CObjContentProvider()
    {
        rowList = new ArrayList<CObjElement>();
    }

    public void addCObj ( CObj f )
    {
        rowList.add ( new CObjElement ( f ) );
    }

    @Override
    public void dispose()
    {

    }

    @Override
    public void inputChanged ( Viewer arg0, Object arg1, Object arg2 )
    {
    }

    @Override
    public Object[] getElements ( Object a )
    {
        return rowList.toArray();
    }

}
