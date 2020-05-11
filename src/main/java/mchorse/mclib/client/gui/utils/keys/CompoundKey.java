package mchorse.mclib.client.gui.utils.keys;

public class CompoundKey implements IKey
{
	public static long lastTime;

	public IKey[] keys;
	public String string;
	public long time = -1;

	public CompoundKey(IKey... keys)
	{
		this.keys = keys;
	}

	@Override
	public String get()
	{
		if (lastTime > time)
		{
			this.time = lastTime;
			this.construct();
		}

		return this.string;
	}

	private void construct()
	{
		StringBuilder builder = new StringBuilder();

		for (IKey key : this.keys)
		{
			builder.append(key.get());
		}

		this.string = builder.toString();
	}

	@Override
	public void set(String string)
	{
		throw new IllegalStateException("Not implemented!");
	}

	public void set(IKey... keys)
	{
		this.keys = keys;
		this.construct();
	}
}