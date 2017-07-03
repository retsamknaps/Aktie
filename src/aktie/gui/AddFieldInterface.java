package aktie.gui;

import org.eclipse.jface.viewers.TableViewer;

import aktie.IdentityCache;
import aktie.data.CObj;
import aktie.index.IndexInterface;

public interface AddFieldInterface
{

    public IndexInterface getIndex();

    public CObj getCommunity();

    public IdentityCache getIdCache();

    public TableViewer getTableViewer();

}
