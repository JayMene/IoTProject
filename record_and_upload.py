from flask import Flask, jsonify
import subprocess
from datetime import datetime
from google.cloud import storage

app = Flask(__name__)

# Set your bucket name and credentials path
BUCKET_NAME = "iotapplication"
CREDENTIALS_PATH = "/home/pi/Downloads/weighty-vertex-448623-a8-e4d0d215f991.json"

@app.route('/start', methods=['GET'])
def start_recording():
    current_time = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    global filename
    filename = f"recording_{current_time}.mp4"

    # Start recording with ffmpeg
    subprocess.Popen(['ffmpeg', '-i', 'http://192.168.0.109:8080/?action=stream', '-c:v', 'copy', filename])
    return f"Recording started, saving to {filename}"

@app.route('/stop', methods=['GET'])
def stop_recording():
    # Stop the recording process
    subprocess.call(['pkill', '-f', 'ffmpeg'])

    # Upload the recording to Google Cloud Storage
    try:
        upload_to_gcs(filename, BUCKET_NAME)
        return jsonify({"status": "success", "message": f"Recording stopped and uploaded as {filename}"})
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)})

def upload_to_gcs(local_file_path, bucket_name):
    """Uploads a file to Google Cloud Storage."""
    # Authenticate using the service account
    storage_client = storage.Client.from_service_account_json(CREDENTIALS_PATH)
    bucket = storage_client.bucket(bucket_name)

    # Generate a unique filename in the bucket
    blob = bucket.blob(local_file_path)
    blob.upload_from_filename(local_file_path)

    print(f"File {local_file_path} uploaded to bucket {bucket_name}.")

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)