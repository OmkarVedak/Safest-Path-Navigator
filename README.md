# Safest Path recommendation

Suggest safest path algorithm Project MVP - CSE 535

# Server

## Setup

### Install dependencies

Go to `server` folder

```sh
cd server
pip install -r requirements.txt
```

### Setup environment

Create `.env` file

```sh
touch .env
```

Insert API_KEY in `.env` file

```
API_KEY=<Your APIKEY here>
FLASK_APP=server.py
```

### Start server

```sh
flask run --host=0.0.0.0
```

## API

### Directions

**Example 1**: Place name

* **Source**: Sydney Town Hall
* **Destination**: Parramatta, NSW

```
GET /directions?source=Sydney Town Hall&destination=Parramatta, NSW
```

**Example 2**: Coordinates

```
GET /directions?source=33.4213414,-111.9252398&destination=33.4294247,-111.9427368
```