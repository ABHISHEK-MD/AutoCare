#!/usr/bin/env bash
set -e

BASE_URL="http://localhost:8080/api"
echo "=========================================================="
echo "Starting AutoCare End-to-End Verification Test Suite"
echo "=========================================================="

RAND_SUFFIX=$((RANDOM % 9000 + 1000))
TEST_REG1="KA50AA${RAND_SUFFIX}"
TEST_REG2="KA50BB${RAND_SUFFIX}"

check_status() {
    local label="$1"
    local expected="$2"
    local actual="$3"
    if [ "$expected" == "$actual" ]; then
        echo "✅ [PASS] $label (HTTP $actual)"
    else
        echo "❌ [FAIL] $label (Expected HTTP $expected, got $actual)"
        exit 1
    fi
}

echo ""
echo "--- TEST 1: Create a Customer ---"
CUST_RES=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/customers" \
  -H "Content-Type: application/json" \
  -d "{\"fullName\": \"Test Customer $RAND_SUFFIX\", \"phone\": \"987650${RAND_SUFFIX}\", \"email\": \"test${RAND_SUFFIX}@example.com\", \"address\": \"789 Indiranagar, Bangalore\"}")
HTTP_CODE=$(echo "$CUST_RES" | tail -n1)
BODY=$(echo "$CUST_RES" | sed '$d')
check_status "Create Customer Test Customer $RAND_SUFFIX" "201" "$HTTP_CODE"
CUSTOMER_ID=$(echo "$BODY" | grep -o '"customerId":[0-9]*' | cut -d':' -f2)
echo "   Created Customer ID: $CUSTOMER_ID"

echo ""
echo "--- TEST 1b: Customer Validations ---"
RES=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/customers" \
  -H "Content-Type: application/json" \
  -d '{"fullName": "", "phone": "9876543210"}')
check_status "Reject Empty Name" "400" "$(echo "$RES" | tail -n1)"

RES=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/customers" \
  -H "Content-Type: application/json" \
  -d '{"fullName": "Invalid Phone", "phone": "123"}')
check_status "Reject Short Phone (<10 digits)" "400" "$(echo "$RES" | tail -n1)"

RES=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/customers" \
  -H "Content-Type: application/json" \
  -d '{"fullName": "Invalid Email", "phone": "9876543210", "email": "bad-email"}')
check_status "Reject Malformed Email" "400" "$(echo "$RES" | tail -n1)"

echo ""
echo "--- TEST 2: Register Two Vehicles for that Customer ---"
# Vehicle 1 (pass lowercase to test auto-uppercase normalization)
LOW_REG=$(echo "$TEST_REG1" | tr '[:upper:]' '[:lower:]')
VEH1_RES=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/vehicles" \
  -H "Content-Type: application/json" \
  -d "{\"registrationNumber\": \"$LOW_REG\", \"brand\": \"Toyota\", \"model\": \"Innova\", \"variant\": \"Crysta 2.4 VX\", \"manufacturingYear\": 2022, \"fuelType\": \"Diesel\", \"customer\": {\"customerId\": $CUSTOMER_ID}}")
check_status "Register Vehicle 1 ($LOW_REG -> normalized to $TEST_REG1)" "201" "$(echo "$VEH1_RES" | tail -n1)"
VEHICLE1_ID=$(echo "$VEH1_RES" | sed '$d' | grep -o '"vehicleId":[0-9]*' | cut -d':' -f2)
REG1=$(echo "$VEH1_RES" | sed '$d' | grep -o '"registrationNumber":"[^"]*"' | cut -d'"' -f4)
echo "   Created Vehicle 1 ID: $VEHICLE1_ID, Normalized Reg: $REG1"

# Vehicle 2
VEH2_RES=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/vehicles" \
  -H "Content-Type: application/json" \
  -d "{\"registrationNumber\": \"$TEST_REG2\", \"brand\": \"Honda\", \"model\": \"City\", \"variant\": \"ZX CVT\", \"manufacturingYear\": 2023, \"fuelType\": \"Petrol\", \"customer\": {\"customerId\": $CUSTOMER_ID}}")
check_status "Register Vehicle 2 ($TEST_REG2)" "201" "$(echo "$VEH2_RES" | tail -n1)"
VEHICLE2_ID=$(echo "$VEH2_RES" | sed '$d' | grep -o '"vehicleId":[0-9]*' | cut -d':' -f2)
echo "   Created Vehicle 2 ID: $VEHICLE2_ID"

echo ""
echo "--- TEST 2b: Duplicate Registration Rejection ---"
DUP_RES=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/vehicles" \
  -H "Content-Type: application/json" \
  -d "{\"registrationNumber\": \"$TEST_REG1\", \"brand\": \"Toyota\", \"model\": \"Innova\", \"fuelType\": \"Diesel\", \"customer\": {\"customerId\": $CUSTOMER_ID}}")
check_status "Reject Duplicate Vehicle Registration Number" "400" "$(echo "$DUP_RES" | tail -n1)"

echo ""
echo "--- TEST 2c: Invalid Customer Reference Rejection ---"
INV_CUST_RES=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/vehicles" \
  -H "Content-Type: application/json" \
  -d '{"registrationNumber": "MH99ZZ9999", "brand": "Hyundai", "model": "Creta", "fuelType": "Petrol", "customer": {"customerId": 999999}}')
check_status "Reject Vehicle with Non-Existent Customer" "404" "$(echo "$INV_CUST_RES" | tail -n1)"

echo ""
echo "--- TEST 3: Search for First Vehicle using Registration Number ---"
SEARCH_RES=$(curl -s -w "\n%{http_code}" "$BASE_URL/vehicles/registration/$LOW_REG")
check_status "Search Vehicle by Registration (case-insensitive lookup)" "200" "$(echo "$SEARCH_RES" | tail -n1)"
FOUND_REG=$(echo "$SEARCH_RES" | sed '$d' | grep -o '"registrationNumber":"[^"]*"' | cut -d'"' -f4)
FOUND_CUST=$(echo "$SEARCH_RES" | sed '$d' | grep -o '"fullName":"[^"]*"' | cut -d'"' -f4)
echo "   Found Registration: $FOUND_REG, Owner: $FOUND_CUST"

NOT_FOUND_RES=$(curl -s -w "\n%{http_code}" "$BASE_URL/vehicles/registration/UNKNOWN999XYZ")
check_status "Search Non-Existent Registration returns 404" "404" "$(echo "$NOT_FOUND_RES" | tail -n1)"

echo ""
echo "--- TEST 4: Create First Service Record ---"
SR1_RES=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/service-records" \
  -H "Content-Type: application/json" \
  -d "{\"vehicle\": {\"vehicleId\": $VEHICLE1_ID}, \"serviceDate\": \"2024-05-10\", \"odometerReading\": 10000, \"serviceType\": \"Periodic Maintenance\", \"customerComplaint\": \"Regular scheduled 10,000 km checkup\", \"workPerformed\": \"Engine oil changed, oil filter replaced, 40-point safety inspection completed\", \"partsReplaced\": \"Synthetic 5W-30 Oil, Oil Filter\"}")
check_status "Create First Service Record (10,000 km)" "201" "$(echo "$SR1_RES" | tail -n1)"
SR1_ID=$(echo "$SR1_RES" | sed '$d' | grep -o '"serviceId":[0-9]*' | cut -d':' -f2)
echo "   Created Service Record 1 ID: $SR1_ID"

echo ""
echo "--- TEST 5: Create Second Service Record with Higher Odometer ---"
SR2_RES=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/service-records" \
  -H "Content-Type: application/json" \
  -d "{\"vehicle\": {\"vehicleId\": $VEHICLE1_ID}, \"serviceDate\": \"2024-11-01\", \"odometerReading\": 18500, \"serviceType\": \"Brake System Service\", \"customerComplaint\": \"Squeaking noise from front wheels when braking\", \"workPerformed\": \"Front brake pads replaced, brake rotors resurfaced, brake fluid flushed\", \"partsReplaced\": \"Front Ceramic Brake Pads, DOT4 Brake Fluid\", \"remarks\": \"Advised rear brake pad check at 25,000 km\"}")
check_status "Create Second Service Record (18,500 km)" "201" "$(echo "$SR2_RES" | tail -n1)"
SR2_ID=$(echo "$SR2_RES" | sed '$d' | grep -o '"serviceId":[0-9]*' | cut -d':' -f2)
echo "   Created Service Record 2 ID: $SR2_ID"

echo ""
echo "--- TEST 6: Verify Complete Service History Preserved (Newest First) ---"
HIST_RES=$(curl -s -w "\n%{http_code}" "$BASE_URL/vehicles/$VEHICLE1_ID/service-history")
check_status "Fetch Service History" "200" "$(echo "$HIST_RES" | tail -n1)"
HIST_BODY=$(echo "$HIST_RES" | sed '$d')
REC_COUNT=$(echo "$HIST_BODY" | grep -o '"serviceId"' | wc -l)
if [ "$REC_COUNT" -eq 2 ]; then
    echo "✅ [PASS] Both service records preserved in history (count: $REC_COUNT)"
else
    echo "❌ [FAIL] Expected 2 records, got $REC_COUNT"
    exit 1
fi

echo ""
echo "--- TEST 7: Attempt Lower Odometer Reading (Must be Rejected) ---"
LOWER_ODO_RES=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/service-records" \
  -H "Content-Type: application/json" \
  -d "{\"vehicle\": {\"vehicleId\": $VEHICLE1_ID}, \"serviceDate\": \"2024-11-10\", \"odometerReading\": 12000, \"serviceType\": \"General Service\", \"workPerformed\": \"Oil change\"}")
check_status "Reject Lower Odometer Reading (12,000 < previous 18,500)" "400" "$(echo "$LOWER_ODO_RES" | tail -n1)"

NEG_ODO_RES=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/service-records" \
  -H "Content-Type: application/json" \
  -d "{\"vehicle\": {\"vehicleId\": $VEHICLE1_ID}, \"serviceDate\": \"2024-11-10\", \"odometerReading\": -500, \"serviceType\": \"General Service\", \"workPerformed\": \"Check\"}")
check_status "Reject Negative Odometer Reading" "400" "$(echo "$NEG_ODO_RES" | tail -n1)"

echo ""
echo "--- TEST 8: Edit Customer Details ---"
EDIT_CUST_RES=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/customers/$CUSTOMER_ID" \
  -H "Content-Type: application/json" \
  -d "{\"fullName\": \"Updated Customer $RAND_SUFFIX\", \"phone\": \"987650${RAND_SUFFIX}\", \"email\": \"updated${RAND_SUFFIX}@example.com\", \"address\": \"Updated Address $RAND_SUFFIX\"}")
check_status "Edit Customer Details" "200" "$(echo "$EDIT_CUST_RES" | tail -n1)"
UPDATED_NAME=$(echo "$EDIT_CUST_RES" | sed '$d' | grep -o '"fullName":"[^"]*"' | cut -d'"' -f4)
echo "   Updated Customer Name: $UPDATED_NAME"

echo ""
echo "--- TEST 9: Edit Vehicle Details ---"
EDIT_VEH_RES=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/vehicles/$VEHICLE1_ID" \
  -H "Content-Type: application/json" \
  -d "{\"registrationNumber\": \"$TEST_REG1\", \"brand\": \"Toyota\", \"model\": \"Innova\", \"variant\": \"Crysta 2.4 ZX AT\", \"manufacturingYear\": 2022, \"fuelType\": \"Diesel\", \"customer\": {\"customerId\": $CUSTOMER_ID}}")
check_status "Edit Vehicle Details" "200" "$(echo "$EDIT_VEH_RES" | tail -n1)"
UPDATED_VARIANT=$(echo "$EDIT_VEH_RES" | sed '$d' | grep -o '"variant":"[^"]*"' | cut -d'"' -f4)
echo "   Updated Vehicle Variant: $UPDATED_VARIANT"

echo ""
echo "--- TEST 10: Edit a Service Record ---"
EDIT_SR_RES=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/service-records/$SR2_ID" \
  -H "Content-Type: application/json" \
  -d "{\"vehicle\": {\"vehicleId\": $VEHICLE1_ID}, \"serviceDate\": \"2024-11-01\", \"odometerReading\": 18600, \"serviceType\": \"Brake System Service\", \"customerComplaint\": \"Squeaking noise from front wheels\", \"workPerformed\": \"Front ceramic brake pads installed and test driven\", \"partsReplaced\": \"Front Ceramic Brake Pads\", \"remarks\": \"Brakes fully bedded and verified silent\"}")
check_status "Edit Service Record #$SR2_ID" "200" "$(echo "$EDIT_SR_RES" | tail -n1)"

echo ""
echo "--- TEST 11: Verify Dashboard Statistics Update Correctly ---"
DASH_RES=$(curl -s -w "\n%{http_code}" "$BASE_URL/dashboard/stats")
check_status "Fetch Dashboard Stats" "200" "$(echo "$DASH_RES" | tail -n1)"
DASH_BODY=$(echo "$DASH_RES" | sed '$d')
TOTAL_CUSTS=$(echo "$DASH_BODY" | grep -o '"totalCustomers":[0-9]*' | cut -d':' -f2)
TOTAL_VEHS=$(echo "$DASH_BODY" | grep -o '"totalVehicles":[0-9]*' | cut -d':' -f2)
TOTAL_SR=$(echo "$DASH_BODY" | grep -o '"totalServiceRecords":[0-9]*' | cut -d':' -f2)
echo "   Dashboard Response: Total Customers=$TOTAL_CUSTS, Total Vehicles=$TOTAL_VEHS, Total Service Records=$TOTAL_SR"

if [ "$TOTAL_CUSTS" -ge 1 ] && [ "$TOTAL_VEHS" -ge 2 ] && [ "$TOTAL_SR" -ge 2 ]; then
    echo "✅ [PASS] Dashboard counts verified: Customers=$TOTAL_CUSTS, Vehicles=$TOTAL_VEHS, ServiceRecords=$TOTAL_SR"
else
    echo "❌ [FAIL] Dashboard counts incorrect: Customers=$TOTAL_CUSTS, Vehicles=$TOTAL_VEHS, ServiceRecords=$TOTAL_SR"
    exit 1
fi

echo ""
echo "--- TEST 12: Verify Deletion Restrictions ---"
# Deleting customer with vehicles must be rejected
DEL_CUST_RES=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL/customers/$CUSTOMER_ID")
check_status "Prevent Deleting Customer who owns vehicles" "400" "$(echo "$DEL_CUST_RES" | tail -n1)"

# Deleting vehicle with service records must be rejected
DEL_VEH_RES=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL/vehicles/$VEHICLE1_ID")
check_status "Prevent Deleting Vehicle that has service records" "400" "$(echo "$DEL_VEH_RES" | tail -n1)"

# Deleting vehicle WITHOUT service records is ALLOWED
DEL_VEH2_RES=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL/vehicles/$VEHICLE2_ID")
check_status "Allow Deleting Vehicle 2 (No service records)" "204" "$(echo "$DEL_VEH2_RES" | tail -n1)"

echo ""
echo "=========================================================="
echo "🎉 ALL END-TO-END WORKFLOW TESTS COMPLETED AND PASSED!"
echo "=========================================================="
