import requests
import json

url = "https://api.xiaomimimo.com/v1/chat/completions"
headers = {
    "Authorization": "Bearer sk-c4nnyneg0nrupszt04mtdd6mk8upgoxtbab7w5l5u2k19c2x",
    "Content-Type": "application/json"
}
payload = {
    "model": "mimo-v2.5",
    "messages": [
        {
            "role": "user",
            "content": "Hello"
        }
    ]
}

try:
    response = requests.post(url, headers=headers, json=payload, timeout=5)
    print("Status:", response.status_code)
    print("Response:", response.text)
except Exception as e:
    print("Error:", e)
