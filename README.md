# Mobile LED Message Detection

###Useful Links
	- configuring opencv in android studio: http://blog.hig.no/gtl/2014/08/28/opencv-and-android-studio/

	- communication standard: http://en.wikipedia.org/wiki/Asynchronous_serial_communication

##Clarifying Questions
	
	1. 
		Q.) How are the LEDs read -- is the application expected to read binary messages conveyed through flashing lights, 
		is it supposed to read static lights, is it supposed to be able to distinguish color?

		A.) Application is expected to get a string of bytes by reading flashing LEDs. If LEDs can flash at frequency unseen by the eye 
		– it would be an advantage, but is low priority at this stage. Using color might be interesting to convey more information, but 
		is low priority at this stage.

	2. 
		Q.) Is the application expected to be able to distinguish between various LED arrangements on different computer types? i.e. the 
		arrangement of LEDs on the front of one computer might be different than that of another computer

		A.) This applies when >1 LED is used for signalling. It is likely this will be needed as amount of information transferred via single 
		LED might be very small. >1 LED is lower priority now, but it would be wise to make provisions to simplify testing this path should it 
		become necessary. For >1 LED we need a heuristic (for example LEDs are numbered left to right, top to bottom etc) for decoding information.

	3. 
		Q.) For the data being transmitted, does the transmission follow a specific standard? If so, what is it? Is the application expected to work 
		with LED information which is already in place, or is part of the project deciding how the computers should convey information through LEDs?

		A.) Yes, latter is part of the project and is, in fact, main priority now. Essentially, proof of concept should answer the following questions fast: 
 
		Achievable data rate / LED (and per camera FPS: 15/30/45/60/90/120) Error rate (or data rate with error correction if it is used) How 
		disruptive to conventional use of LEDs (i.e. can this be done completely transparently with LED appearing on/off is its main function would dictate)  
 
		Same as above but when using handheld camera (think of imperfect orientation, moving glare, some shaking, time limit – it’d be awkward 
		to hold camera steady for longer than probably ~5-10 sec – can find this limit experimentally as well)

	4. 
		Q.) What should the app identify? e.g. machine specifications, machine status, error codes, etc.

		A.) This is TBD, but initial idea is something along the lines: what device it is (name / type / status – i.e. if something is abnormal
		 – what it is, can be derived from syslog or other sources of device state information – depends on how much information can be transferred)
