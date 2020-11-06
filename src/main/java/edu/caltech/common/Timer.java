package edu.caltech.common;

public class Timer {
	long startTime_;
	public Timer() {
		timerStart();
	}
	public void timerStart() {
		startTime_=System.currentTimeMillis();		
	}
	public long getTimeElapsed() {
		return System.currentTimeMillis()-startTime_;
	}


}
