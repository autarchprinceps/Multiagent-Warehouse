console.log(JSON.stringify({
	shelves: [
		{
			uid: 0,
			products: [
				{
					name: "ROTOR",
					stock: {
						current: 100
					}
				}, {
					name: "SCREW",
					stock: {
						current: 100
					}
				}, {
					name: "STABILISER",
					stock: {
						current: 100
					}
				}
			]
		}, {
			uid: 1,
			products: [
				{
					name: "BATTERY",
					stock: {
						current: 100
					}
				}, {
					name: "CIRCUIT",
					stock: {
						current: 100
					}
				}, {
					name: "TUNER",
					stock: {
						current: 100
					}
				}
			],
		}, {
			uid: 2,
			products: [
				{
					name: "ENGINE",
					stock: {
						current: 100
					}
				}, {
					name: "SCREW",
					stock: {
						current: 100
					}
				}, {
					name: "CHARGER",
					stock: {
						current: 100
					}
				}
			]
		}, {
			uid: 3,
			products: [
				{
					name: "CASE",
					stock: {
						current: 100
					}
				}, {
					name: "CAMERA",
					stock: {
						current: 100
					}
				}, {
					name: "SPOTLIGHT",
					stock: {
						current: 100
					}
				}
			]
		}, {
			uid: 4,
			products: [
				{
					name: "CASE",
					stock: {
						current: 100
					}
				}, {
					name: "CHARGER",
					stock: {
						current: 100
					}
				}, {
					name: "BATTERY",
					stock: {
						current: 100
					}
				}
			]
		}
	],
	pickers: [{uid: 0}, {uid: 1}, {uid: 2}],
	robots: [{uid: 0}, {uid: 1}, {uid: 2}],
	orders: [
		{
			uid: 0,
			products: [
				{
					name: "ROTOR",
					quantity: 8
				}, {
					name: "BATTERY",
					quantity: 2
				}, {
					name: "CAMERA",
					quantity: 1
				}, {
					name: "CASE",
					quantity: 1
				}, {
					name: "ENGINE",
					quantity: 8
				}, {
					name: "SCREW",
					quantity: 40
				}
			]
		}, {
			uid: 1,
			products: [
				{
					name: "CIRCUIT",
					quantity: "30"
				}
			]
		}
	]
}));
