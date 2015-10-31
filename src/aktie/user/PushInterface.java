package aktie.user;

import java.util.List;

import aktie.data.CObj;

public interface PushInterface
{

    public void push ( CObj fromid, CObj o );

    public List<String> getConnectedIds ( CObj fromid );

    public void push ( CObj fromid, String to, CObj o );

}
