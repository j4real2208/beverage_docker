function loadBeverages() {
    console.log('Loading beverages for customer view');
    fetch('/api/beverages')
        .then(res => {
            if (!res.ok) throw new Error('Failed to load beverages');
            return res.json();
        })
        .then(data => {
            const bottles = (Array.isArray(data) ? data : []).filter(b => b.type === 'bottle');
            const crates = (Array.isArray(data) ? data : []).filter(b => b.type === 'crate');
            const bottleList = document.getElementById('bottle-list');
            const crateList = document.getElementById('crate-list');
            const errorDiv = document.getElementById('beverage-error');
            bottleList.innerHTML = '';
            crateList.innerHTML = '';
            errorDiv.style.display = 'none';
            bottles.forEach(b => {
                console.log(b);
                const li = document.createElement('li');
                li.innerText = b.name ? `${b.name} - $${b.price}` : JSON.stringify(b);
                li.onclick = () => showBeverageDetails(b);
                bottleList.appendChild(li);
            });
            crates.forEach(c => {
                const li = document.createElement('li');
                let bottleInfo = '';
                if (c.bottle && typeof c.bottle === 'object') {
                    bottleInfo = c.bottle.name ? `<b>${c.bottle.name}</b>` : JSON.stringify(c.bottle);
                    if (c.bottle.volume) bottleInfo += `, <span style='color:#555;'>${c.bottle.volume}L</span>`;
                    if (c.bottle.isAlcoholic !== undefined) bottleInfo += c.bottle.isAlcoholic ? ', <span style="color:#0074d9">Alcoholic</span>' : ', <span style="color:#2ecc40">Non-alcoholic</span>';
                }
                li.innerHTML = bottleInfo
                    ? `Crate of ${bottleInfo} <span style='color:#888;'>(${c.noOfBottles} bottles)</span> - <span style='color:#0074d9;'>$${c.price}</span>`
                    : JSON.stringify(c);
                li.onclick = () => showBeverageDetails(c);
                crateList.appendChild(li);
            });
        })
        .catch(err => {
            const bottleList = document.getElementById('bottle-list');
            const crateList = document.getElementById('crate-list');
            const errorDiv = document.getElementById('beverage-error');
            bottleList.innerHTML = '';
            crateList.innerHTML = '';
            errorDiv.innerText = 'Error loading beverages';
            errorDiv.style.display = 'block';
            console.error(err);
        });
}

function showBeverageDetails(beverage) {
    const details = document.getElementById('beverage-details');
    details.innerHTML = '';
    for (const key in beverage) {
        if (typeof beverage[key] === 'object') {
            details.innerHTML += `<div><b>${key}:</b> ${JSON.stringify(beverage[key])}</div>`;
        } else {
            details.innerHTML += `<div><b>${key}:</b> <span id="edit-${key}">${beverage[key]}</span></div>`;
        }
    }
    // Always show edit/delete buttons
    details.innerHTML += `<button onclick='editBeverage(${JSON.stringify(beverage)})'>Edit</button>`;
    details.innerHTML += `<button onclick='deleteBeverage(${beverage.id})'>Delete</button>`;
    details.innerHTML += `<button onclick='closeView()'>Close View</button>`;
    details.style.display = 'block';
}

function editBeverage(beverage) {
    const details = document.getElementById('beverage-details');
    details.innerHTML = '';
    for (const key in beverage) {
        if (typeof beverage[key] === 'object') {
            details.innerHTML += `<div><b>${key}:</b> ${JSON.stringify(beverage[key])}</div>`;
        } else if (key !== 'id') {
            details.innerHTML += `<div><b>${key}:</b> <input id='input-${key}' value='${beverage[key]}' /></div>`;
        } else {
            details.innerHTML += `<div><b>${key}:</b> ${beverage[key]}</div>`;
        }
    }
    details.innerHTML += `<button onclick='saveBeverage(${beverage.id})'>Save</button>`;
    details.innerHTML += `<button onclick='closeView()'>Cancel</button>`;
}

function closeView() {
    const details = document.getElementById('beverage-details');
    details.style.display = 'none';
    loadBeverages();
}

function saveBeverage(id) {
    const details = document.getElementById('beverage-details');
    const inputs = details.querySelectorAll('input');
    const updated = { id };
    inputs.forEach(input => {
        const key = input.id.replace('input-', '');
        let value = input.value;
        // Try to convert to number if appropriate
        if (!isNaN(value) && value.trim() !== '') {
            value = Number(value);
        }
        if (value === 'true') value = true;
        if (value === 'false') value = false;
        updated[key] = value;
    });
    fetch(`/management/beverages/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(updated)
    }).then(() => {
        loadBeverages();
        details.style.display = 'none';
    });
}

function deleteBeverage(id) {
    fetch(`/management/beverages/${id}`, { method: 'DELETE' })
        .then(() => {
            loadBeverages();
            document.getElementById('beverage-details').style.display = 'none';
        });
}


function showAddForm() {
    const addForm = document.getElementById('add-form');
    addForm.style.display = 'block';
}

function cancelAddForm() {
    const addForm = document.getElementById('add-form');
    addForm.style.display = 'none';
}

function addBeverage() {
    const name = document.getElementById('add-name').value;
    const price = parseFloat(document.getElementById('add-price').value);
    const volume = parseFloat(document.getElementById('add-volume').value);
    const supplier = document.getElementById('add-supplier').value;
    const volumePercent = parseFloat(document.getElementById('add-volumePercent').value);
    const inStock = parseInt(document.getElementById('add-inStock').value, 10);
    const type = document.getElementById('add-type').value;
    const isAlcoholic = document.getElementById('add-isAlcoholic').checked;

    const newBeverage = {
        name,
        price,
        volume,
        supplier,
        volumePercent,
        inStock,
        type,
        isAlcoholic
    };

    fetch('/management/beverages', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(newBeverage)
    })
    .then(res => {
        if (res.status === 200 || res.status === 201) {
            return res.json();
        } else {
            throw new Error(`Unexpected response status: ${res.status}`);
        }
    })
    .then(() => {
        loadBeverages();
        cancelAddForm();
    })
    .catch(err => {
        const errorDiv = document.getElementById('beverage-error');
        errorDiv.innerText = `Error adding beverage: ${err.message}`;
        errorDiv.style.display = 'block';
        console.error(err);
    });
}

loadBeverages();