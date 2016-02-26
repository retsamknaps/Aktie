package aktie.gui;

import org.eclipse.jface.viewers.TableViewer;

import aktie.data.CObj;
import aktie.index.Index;

public interface AddFieldInterface
{

    public Index getIndex();

    public CObj getCommunity();

    public IdentityCache getIdCache();

    public TableViewer getTableViewer();

}
