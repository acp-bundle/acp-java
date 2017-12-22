package co.clai.html;

public class GenericBuffer extends AbstractRenderer {

	public GenericBuffer(String begin) {
		super(begin);
	}

	@Override
	public void write(Builder b) {
		appendData(b.finish());
	}

}
