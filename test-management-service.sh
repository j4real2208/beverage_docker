#!/bin/bash

# Test: Add a bottle
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Cola",
    "volume": 0.5,
    "isAlcoholic": false,
    "volumePercent": 0.0,
    "price": 1.99,
    "supplier": "TestSupplier",
    "inStock": 100,
    "type" : "bottle"
  }' \
  http://localhost:8090/management/beverages

echo -e "\n---\n"

# Test: Get all beverages
curl -X GET http://localhost:8090/management/beverages

echo -e "\n---\n"

# Test: Update a beverage (id=1)
curl -X PUT \
  -H "Content-Type: application/json" \
  -d '{
      "volume" : 0.5,
      "isAlcoholic" : false,
      "price" : 1.5,
      "supplier" : "CocaCola&Co",
      "name" : "Cola Bottle",
      "volumePercent" : 0.0,
      "inStock" : 100,
      "id" : 1,
      "type" : "bottle"
  }' \
  http://localhost:8090/management/beverages/1

echo -e "\n---\n"

# Test: Delete a beverage (id=1)
curl -X DELETE http://localhost:8090/management/beverages/2

echo -e "\n---\n"

# Test: Get all beverages again
curl -X GET http://localhost:8090/management/beverages

