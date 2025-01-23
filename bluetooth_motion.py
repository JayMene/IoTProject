import time
from grove.gpio import GPIO
from bluedot.btcomm import BluetoothServer

# Define the pin for the PIR motion sensor (always set to pin 5)
MOTION_SENSOR_PIN = 5
BUZZER_PIN = 12
BUTTON_PIN = 18

client_connected = False 

class GroveMiniPIRMotionSensor(GPIO):
    def __init__(self, pin):
        super(GroveMiniPIRMotionSensor, self).__init__(pin, GPIO.IN)
        self._on_detect = None

    @property
    def on_detect(self):
        return self._on_detect

    @on_detect.setter
    def on_detect(self, callback):
        if not callable(callback):
            return

        if self.on_event is None:
            self.on_event = self._handle_event

        self._on_detect = callback

    def _handle_event(self, pin, value):
        if value:
            if callable(self._on_detect):
                self._on_detect()




def on_client_connect():
    """
    This function is triggered when a Bluetooth client connects.
    Starts the motion detection check.
    """
    global client_connected
    print("Client connected")
    print("Starting motion detection...")
    client_connected = True
    time.sleep(1)


def on_client_disconnect():
    global client_connected
    client_connected = False
    """
    This function is triggered when a Bluetooth client disconnects.
    Can be used to stop or pause motion detection if needed.
    """
    print("Client disconnected")


def motion_callback():
    global client_connected
    """
    This function is called when motion is detected.
    It activates the buzzer for a short interval.
    """
    global server 
    print("Motion detected!")
    
    if client_connected:
        print("Sending: Motion Detected")
        server.send("Motion Detected")
        print("Message sent successfully")
    # Turn the buzzer on for a short interval (e.g., 1 second)
    buzzer.write(1)  # Turn on buzzer (PWM signal)
    time.sleep(1)    # Wait for 1 second
    buzzer.write(0)  # Turn off buzzer
    
def button_callback():
    global client_connected
    print("Button pressed!")
    
    if client_connected:
        print("Sending: Button Pressed")
        server.send("Button Pressed")
        print("Message sent successfully")
        
def dummy_data_received(data):
    pass
def main():
    # Create the PIR motion sensor instance on pin 5
    global motion_sensor, buzzer, server, button
    motion_sensor = GroveMiniPIRMotionSensor(MOTION_SENSOR_PIN)
    
    buzzer = GPIO(BUZZER_PIN, GPIO.OUT)
    button = GPIO(BUTTON_PIN, GPIO.IN)

    motion_sensor.on_detect = motion_callback
    button.on_event = lambda pin, value: button_callback() if value else None
    # Start the Bluetooth server
    server = BluetoothServer(
        dummy_data_received,
        when_client_connects=on_client_connect,
        when_client_disconnects=on_client_disconnect
    )
    try:
        # Keep the Bluetooth server running indefinitely
        while True:
            time.sleep(1)
        
    except KeyboardInterrupt:
        print("Exiting...")
        server.stop()  # Ensure the server is stopped before exiting

if __name__ == '__main__':
    main()

